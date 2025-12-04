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
