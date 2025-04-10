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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.forgerock.openam.auth.nodes.EmailSuspendNode;

/**
 * {@link EmailSuspendNode} Builder.
 */
public class EmailSuspendBuilder  extends AbstractNodeBuilder implements EmailSuspendNode.Config {
    private static final String DEFAULT_DISPLAY_NAME = "Email Suspend";

    private String emailTemplateName;
    private String emailAttribute;
    private Map<Locale, String> emailSuspendMessage;
    private boolean objectLookup;
    private String identityAttribute;

    /**
     * The EmailSuspendBuilder constructor.
     */
    public EmailSuspendBuilder() {
        super(DEFAULT_DISPLAY_NAME, EmailSuspendNode.class);
    }

    /**
     * Sets the emailTemplateName.
     *
     * @param emailTemplateName the name of the IDM emailTemplate to send
     * @return the email template
     */
    public EmailSuspendBuilder emailTemplateName(String emailTemplateName) {
        this.emailTemplateName = emailTemplateName;
        return this;
    }

    /**
     * Sets the emailAttribute.
     *
     * @param emailAttribute the email attribute field
     * @return the emailAttribute
     */
    public EmailSuspendBuilder emailAttribute(String emailAttribute) {
        this.emailAttribute = emailAttribute;
        return this;
    }

    /**
     * Sets the emailSuspendMessage.
     *
     * @param emailSuspendMessage the localized message to show when suspending the tree
     * @return the suspend message
     */
    public EmailSuspendBuilder emailSuspendMessage(Map<Locale, String> emailSuspendMessage) {
        this.emailSuspendMessage = emailSuspendMessage;
        return this;
    }

    /**
     * Sets whether to perform an object lookup.
     *
     * @param objectLookup true if object lookup should occur, otherwise false
     * @return return objectlookup boolean
     */
    public EmailSuspendBuilder objectLookup(boolean objectLookup) {
        this.objectLookup = objectLookup;
        return this;
    }

    /**
     * Sets the identityAttribute.
     *
     * @param identityAttribute the identityAttribute name
     * @return the identityAttribute
     */
    public EmailSuspendBuilder identityAttribute(String identityAttribute) {
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
    public Map<Locale, String> emailSuspendMessage() {
        return emailSuspendMessage;
    }

    @Override
    public boolean objectLookup() {
        return objectLookup;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }

    @Override
    public Optional<Duration> suspendDuration() {
        return Optional.empty();
    }
}
