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

import org.forgerock.openam.auth.nodes.CreatePasswordNode;

/**
 * A CreatePasswordNode builder.
 */
public class CreatePasswordBuilder extends AbstractNodeBuilder implements CreatePasswordNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Create Password";
    private int minPasswordLength;

    /**
     * A CreatePasswordBuilder constructor.
     */
    public CreatePasswordBuilder() {
        super(DEFAULT_DISPLAY_NAME, CreatePasswordNode.class);
    }

    /**
     * A CreatePasswordBuilder constructor.
     * @param minPasswordLength Length of password to generate.
     * @return this builder.
     */
    public CreatePasswordBuilder minPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
        return this;
    }

    @Override
    public int minPasswordLength() {
        return this.minPasswordLength;
    }

}
