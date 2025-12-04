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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.SocialFacebookNode;

/**
 * A SocialFacebookNode builder.
 */
public class SocialFacebookBuilder extends AbstractNodeBuilder implements SocialFacebookNode.FacebookOAuth2Config {

    private static final String DEFAULT_DISPLAY_NAME = "Facebook Social Authentication";
    private String clientId;
    private String clientSecret;
    /**
     * A SocialFacebook constructor.
     */
    public SocialFacebookBuilder() {
        super(DEFAULT_DISPLAY_NAME, SocialFacebookNode.class);
    }

    /**
     * Sets the clientId.
     *
     * @param clientId the clientId.
     * @return this builder.
     */
    public SocialFacebookBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    /**
     * Sets the clientSecret.
     *
     * @param clientSecret the clientSecret.
     * @return this builder.
     */
    public SocialFacebookBuilder clientSecret(String clientSecret) {
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
