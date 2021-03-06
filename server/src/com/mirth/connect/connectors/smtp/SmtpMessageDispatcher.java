/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * http://www.mirthcorp.com
 *
 * The software in this package is published under the terms of the MPL
 * license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package com.mirth.connect.connectors.smtp;

import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.TemplateValueReplacer;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

import com.mirth.connect.model.MessageObject;
import com.mirth.connect.server.Constants;
import com.mirth.connect.server.controllers.AlertController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.MessageObjectController;
import com.mirth.connect.server.controllers.MonitoringController;
import com.mirth.connect.server.controllers.MonitoringController.ConnectorType;
import com.mirth.connect.server.controllers.MonitoringController.Event;

public class SmtpMessageDispatcher extends AbstractMessageDispatcher {
    protected SmtpConnector connector;
    private final MessageObjectController messageObjectController = ControllerFactory.getFactory().createMessageObjectController();
    private final MonitoringController monitoringController = ControllerFactory.getFactory().createMonitoringController();
    private final AlertController alertController = ControllerFactory.getFactory().createAlertController();
    private final TemplateValueReplacer replacer = new TemplateValueReplacer();
    private final ConnectorType connectorType = ConnectorType.WRITER;

    public SmtpMessageDispatcher(SmtpConnector connector) {
        super(connector);
        this.connector = connector;
    }

    @Override
    public void doDispatch(UMOEvent event) throws Exception {
        monitoringController.updateStatus(connector, connectorType, Event.BUSY);
        MessageObject mo = messageObjectController.getMessageObjectFromEvent(event);

        if (mo == null) {
            return;
        }

        try {
            Email email = null;

            if (connector.isHtml()) {
                email = new HtmlEmail();
            } else {
                email = new MultiPartEmail();
            }

            email.setCharset(connector.getCharsetEncoding());

            email.setHostName(replacer.replaceValues(connector.getSmtpHost(), mo));

            try {
                email.setSmtpPort(Integer.parseInt(replacer.replaceValues(connector.getSmtpPort(), mo)));
            } catch (NumberFormatException e) {
                // Don't set if the value is invalid
            }

            try {
                email.setSocketConnectionTimeout(Integer.parseInt(replacer.replaceValues(connector.getTimeout(), mo)));
            } catch (NumberFormatException e) {
                // Don't set if the value is invalid
            }

            if ("SSL".equalsIgnoreCase(connector.getEncryption())) {
                email.setSSL(true);
            } else if ("TLS".equalsIgnoreCase(connector.getEncryption())) {
                email.setTLS(true);
            }

            if (connector.isAuthentication()) {
                email.setAuthentication(replacer.replaceValues(connector.getUsername(), mo), replacer.replaceValues(connector.getPassword(), mo));
            }

            /*
             * NOTE: There seems to be a bug when calling setTo with a List
             * (throws a java.lang.ArrayStoreException), so we are using addTo
             * instead.
             */

            for (String to : replaceValuesAndSplit(connector.getTo(), mo)) {
                email.addTo(to);
            }

            // Currently unused
            for (String cc : replaceValuesAndSplit(connector.cc(), mo)) {
                email.addCc(cc);
            }

            // Currently unused
            for (String bcc : replaceValuesAndSplit(connector.getBcc(), mo)) {
                email.addBcc(bcc);
            }

            // Currently unused
            for (String replyTo : replaceValuesAndSplit(connector.getReplyTo(), mo)) {
                email.addReplyTo(replyTo);
            }

            for (Entry<String, String> header : connector.getHeaders().entrySet()) {
                email.addHeader(replacer.replaceValues(header.getKey(), mo), replacer.replaceValues(header.getValue(), mo));
            }

            email.setFrom(replacer.replaceValues(connector.getFrom(), mo));
            email.setSubject(replacer.replaceValues(connector.getSubject(), mo));

            String body = replacer.replaceValues(connector.getBody(), mo);

            if (connector.isHtml()) {
                ((HtmlEmail) email).setHtmlMsg(body);
            } else {
                email.setMsg(body);
            }

            /*
             * If the MIME type for the attachment is missing, we display a
             * warning and set the content anyway. If the MIME type is of type
             * "text" or "application/xml", then we add the content. If it is
             * anything else, we assume it should be Base64 decoded first.
             */
            for (Attachment attachment : connector.getAttachments()) {
                String name = replacer.replaceValues(attachment.getName(), mo);
                String mimeType = replacer.replaceValues(attachment.getMimeType(), mo);
                String content = replacer.replaceValues(attachment.getContent(), mo);

                byte[] bytes;

                if (StringUtils.indexOf(mimeType, "/") < 0) {
                    logger.warn("valid MIME type is missing for email attachment: \"" + name + "\", using default of text/plain");
                    attachment.setMimeType("text/plain");
                    bytes = content.getBytes();
                } else if ("application/xml".equalsIgnoreCase(mimeType) || StringUtils.startsWith(mimeType, "text/")) {
                    logger.debug("text or XML MIME type detected for attachment \"" + name + "\"");
                    bytes = content.getBytes();
                } else {
                    logger.debug("binary MIME type detected for attachment \"" + name + "\", performing Base64 decoding");
                    bytes = Base64.decodeBase64(content);
                }

                ((MultiPartEmail) email).attach(new ByteArrayDataSource(bytes, mimeType), name, null);
            }

            /*
             * From the Commons Email JavaDoc: send returns
             * "the message id of the underlying MimeMessage".
             */
            String response = email.send();
            messageObjectController.setSuccess(mo, response, null);
        } catch (EmailException e) {
            alertController.sendAlerts(connector.getChannelId(), Constants.ERROR_402, "Error sending email message.", e);
            messageObjectController.setError(mo, Constants.ERROR_402, "Error sending email message.", e, null);
            connector.handleException(new Exception(e));
        } finally {
            monitoringController.updateStatus(connector, connectorType, Event.DONE);
        }
    }

    /**
     * Takes a comma-separated list of email addresses and returns a String[] of
     * individual addresses with replaced values.
     * 
     * @param addresses
     *            A comma-separated list of email addresses
     * @param mo
     *            A MessageObject
     * @return A String[] of individual adresses, or an empty String[] if
     *         addresses is blank
     */
    private String[] replaceValuesAndSplit(String addresses, MessageObject mo) {
        if (StringUtils.isNotBlank(addresses)) {
            return StringUtils.split(replacer.replaceValues(addresses, mo), ",");
        } else {
            return new String[0];
        }
    }

    @Override
    public void doDispose() {

    }

    @Override
    public UMOMessage doSend(UMOEvent event) throws Exception {
        doDispatch(event);
        return event.getMessage();
    }

    @Override
    public Object getDelegateSession() throws UMOException {
        return null;
    }

    @Override
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        return null;
    }

}
