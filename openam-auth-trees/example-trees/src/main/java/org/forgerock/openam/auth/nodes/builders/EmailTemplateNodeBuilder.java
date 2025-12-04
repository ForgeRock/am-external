/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.EmailTemplateNode;

/**
 * Email Template Node Builder.
 */
public class EmailTemplateNodeBuilder extends AbstractNodeBuilder implements EmailTemplateNode.Config {

    private String emailTemplateName;
    private String emailAttribute;
    private String identityAttribute;

    /**
     * The EmailTemplateNodeBuilder constructor.
     */
    public EmailTemplateNodeBuilder() {
        super("EmailTemplateNode", EmailTemplateNode.class);
    }

    /**
     * Sets the emailTemplate.
     * @param emailTemplateName the emailTemplate name.
     * @return the emailTemplate
     */
    public EmailTemplateNodeBuilder emailTemplateName(String emailTemplateName) {
        this.emailTemplateName = emailTemplateName;
        return this;
    }

    /**
     * Sets the emailAttribute.
     * @param emailAttribute the emailAttribute name.
     * @return the emailAttribute
     */
    public EmailTemplateNodeBuilder emailAttribute(String emailAttribute) {
        this.emailAttribute = emailAttribute;
        return this;
    }

    /**
     * Sets the identityAttribute.
     * @param identityAttribute the identityAttribute name.
     * @return the identityAttribute
     */
    public EmailTemplateNodeBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    @Override
    public String emailTemplateName() {
        return emailTemplateName;
    }

    @Override
    public String emailAttribute() {
        return emailAttribute;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }
}
