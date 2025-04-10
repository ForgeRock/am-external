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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.AttributePresentDecisionNode;

/**
 * An {@link AttributePresentDecisionNode} builder.
 */
public class AttributePresentDecisionBuilder extends AbstractNodeBuilder
        implements AttributePresentDecisionNode.Config {

    /**
     * Default display name.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Attribute Present Decision";
    private String presentAttribute;
    private String identityAttribute;

    /** {@link AttributePresentDecisionBuilder} constructor. */
    public AttributePresentDecisionBuilder() {
        super(DEFAULT_DISPLAY_NAME, AttributePresentDecisionNode.class);
    }

    @Override
    public String presentAttribute() {
        return presentAttribute;
    }

    /**
     * Sets the present attribute.
     *
     * @param presentAttribute the attribute to check if present
     * @return this builder
     */
    public AttributePresentDecisionBuilder setPresentAttribute(String presentAttribute) {
        this.presentAttribute = presentAttribute;
        return this;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }

    /**
     * Sets the identity attribute.
     *
     * @param identityAttribute the identity attribute
     * @return this builder
     */
    public AttributePresentDecisionBuilder setIdentityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

}
