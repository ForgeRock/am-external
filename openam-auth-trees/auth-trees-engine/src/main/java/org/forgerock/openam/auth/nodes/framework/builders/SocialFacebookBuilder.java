/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

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