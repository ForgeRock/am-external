/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.SetSuccessUrlNode;

/**
 * A SetSuccessUrlNode builder.
 */
public class SetSuccessUrlBuilder extends AbstractNodeBuilder implements SetSuccessUrlNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Set Success URL";
    private static String successUrl = "https://www.forgerock.com/";
    /**
     * A SetSuccessUrlNode constructor.
     */
    public SetSuccessUrlBuilder() {
        super(DEFAULT_DISPLAY_NAME, SetSuccessUrlNode.class);
    }

    /**
     * Sets the successUrl.
     *
     * @param successUrl the success URL.
     * @return this builder.
     */
    public SetSuccessUrlBuilder successUrl(String successUrl) {
        this.successUrl = successUrl;
        return this;
    }


    @Override
    public String successUrl() {
        return this.successUrl;
    }
}