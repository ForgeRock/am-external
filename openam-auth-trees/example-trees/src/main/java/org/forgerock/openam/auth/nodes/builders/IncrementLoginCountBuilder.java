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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.IncrementLoginCountNode;

/**
 * The IncrementLoginCountNode Builder.
 */
public class IncrementLoginCountBuilder extends AbstractNodeBuilder implements IncrementLoginCountNode.Config {
    private static final String DEFAULT_DISPLAY_NAME = "Increment Login Count";

    private String identityAttribute;

    /**
     * The IncrementLoginCountBuilder constructor.
     */
    public IncrementLoginCountBuilder() {
        super(DEFAULT_DISPLAY_NAME, IncrementLoginCountNode.class);
    }

    /**
     * Sets the attribute to query the IDM object by.
     *
     * @param identityAttribute the identity attribute of the IDM object
     * @return this builder
     */
    public IncrementLoginCountBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    @Override
    public String identityAttribute() {
        return this.identityAttribute;
    }
}
