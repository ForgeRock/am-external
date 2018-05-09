/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

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