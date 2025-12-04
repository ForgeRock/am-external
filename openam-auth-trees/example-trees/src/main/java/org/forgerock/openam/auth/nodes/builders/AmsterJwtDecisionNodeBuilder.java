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
 * Copyright 2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.amster.AmsterJwtDecisionNode;

/**
 * An AmsterJwtDecisionNode builder.
 */
public class AmsterJwtDecisionNodeBuilder extends AbstractNodeBuilder implements AmsterJwtDecisionNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Amster Jwt Decision Node";

    private String authorizedKeys;

    /**
     * An AmsterJwtDecisionNodeBuilder constructor.
     */
    public AmsterJwtDecisionNodeBuilder() {
        super(DEFAULT_DISPLAY_NAME, AmsterJwtDecisionNode.class);
    }

    /**
     * Sets the authorizedKeys.
     * @param authorizedKeys the authorizedKeys name.
     * @return the authorizedKeys
     */
    public AmsterJwtDecisionNodeBuilder authorizedKeys(String authorizedKeys) {
        this.authorizedKeys = authorizedKeys;
        return this;
    }

    @Override
    public String authorizedKeys() {
        return authorizedKeys;
    }
}
