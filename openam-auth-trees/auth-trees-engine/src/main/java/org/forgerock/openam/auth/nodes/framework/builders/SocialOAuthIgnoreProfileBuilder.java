/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.SocialOAuthIgnoreProfileNode;

/**
 * A SocialOAuthIgnoreProfile builder.
 */
public class SocialOAuthIgnoreProfileBuilder extends
        AbstractNodeBuilder implements SocialOAuthIgnoreProfileNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Social Ignore Profile";

    /**
     * A SocialOAuthIgnoreProfile constructor.
     */
    public SocialOAuthIgnoreProfileBuilder() {
        super(DEFAULT_DISPLAY_NAME, SocialOAuthIgnoreProfileNode.class);
    }
}