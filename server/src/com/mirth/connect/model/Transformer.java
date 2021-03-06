/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * http://www.mirthcorp.com
 *
 * The software in this package is published under the terms of the MPL
 * license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package com.mirth.connect.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.ObjectUtils;

import com.mirth.connect.model.MessageObject.Protocol;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.basic.Base64StringConverter;

/**
 * A Transformer represents a script which is executed on each message passing
 * through the Connector with which the transformer is associated.
 * 
 */

@XStreamAlias("transformer")
public class Transformer implements Serializable {
    private List<Step> steps;

    @XStreamConverter(Base64StringConverter.class)
    private String inboundTemplate;

    @XStreamConverter(Base64StringConverter.class)
    private String outboundTemplate;

    private Protocol inboundProtocol;
    private Protocol outboundProtocol;
    private Properties inboundProperties;
    private Properties outboundProperties;

    public Transformer() {
        this.steps = new ArrayList<Step>();
    }

    public Protocol getInboundProtocol() {
        return this.inboundProtocol;
    }

    public void setInboundProtocol(Protocol inboundProtocol) {
        this.inboundProtocol = inboundProtocol;
    }

    public String getInboundTemplate() {
        return inboundTemplate;
    }

    public void setInboundTemplate(String inboundTemplate) {
        this.inboundTemplate = inboundTemplate;
    }

    public Protocol getOutboundProtocol() {
        return outboundProtocol;
    }

    public void setOutboundProtocol(Protocol outboundProtocol) {
        this.outboundProtocol = outboundProtocol;
    }

    public String getOutboundTemplate() {
        return outboundTemplate;
    }

    public void setOutboundTemplate(String outboundTemplate) {
        this.outboundTemplate = outboundTemplate;
    }

    public List<Step> getSteps() {
        return this.steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        if (!(that instanceof Transformer)) {
            return false;
        }

        Transformer transformer = (Transformer) that;

        return ObjectUtils.equals(this.getSteps(), transformer.getSteps()) && ObjectUtils.equals(this.getInboundTemplate(), transformer.getInboundTemplate()) && ObjectUtils.equals(this.getOutboundTemplate(), transformer.getOutboundTemplate()) && ObjectUtils.equals(this.getInboundProtocol(), transformer.getInboundProtocol()) && ObjectUtils.equals(this.getOutboundProtocol(), transformer.getOutboundProtocol()) && ObjectUtils.equals(this.getInboundProperties(), transformer.getInboundProperties()) && ObjectUtils.equals(this.getOutboundProperties(), transformer.getOutboundProperties());
    }

    public Properties getInboundProperties() {
        return inboundProperties;
    }

    public void setInboundProperties(Properties inboundProperties) {
        this.inboundProperties = inboundProperties;
    }

    public Properties getOutboundProperties() {
        return outboundProperties;
    }

    public void setOutboundProperties(Properties outboundProperties) {
        this.outboundProperties = outboundProperties;
    }
}
