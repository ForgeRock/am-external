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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.SocialGoogleNode;

/**
 * A SocialGoogleNode builder.
 */
public class SocialGoogleBuilder extends AbstractNodeBuilder implements SocialGoogleNode.GoogleOAuth2Config {

    private static final String DEFAULT_DISPLAY_NAME = "Google Social Authentication";
    private String clientId;
    private String clientSecret;
    /**
     * A SocialGoogle constructor.
     */
    public SocialGoogleBuilder() {
        super(DEFAULT_DISPLAY_NAME, SocialGoogleNode.class);
    }

    /**
     * Sets the clientId.
     *
     * @param clientId the clientId.
     * @return this builder.
     */
    public SocialGoogleBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * Sets the clientSecret.
     *
     * @param clientSecret the clientSecret.
     * @return this builder.
     */
    public SocialGoogleBuilder clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    @Override
    public String clientId() {
        return this.clientId;
    }

    @Override
    public char[] clientSecret() {
        return this.clientSecret.toCharArray();
    }
}
