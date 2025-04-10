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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.ValidatedUsernameNode;

/**
 * A {@link ValidatedUsernameNode} builder.
 */
public class ValidatedUsernameBuilder extends AbstractNodeBuilder implements ValidatedUsernameNode.Config {
    /**
     * Default display name.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Platform Username";
    private Boolean validateInput;
    private String usernameAttribute;

    /**
     * A {@link ValidatedUsernameBuilder} constructor.
     */
    public ValidatedUsernameBuilder() {
        super(DEFAULT_DISPLAY_NAME, ValidatedUsernameNode.class);
    }

    /**
     * Sets the validateInput.
     * @param validateInput whether to validate input
     * @return validateInput
     */
    public ValidatedUsernameBuilder validateInput(Boolean validateInput) {
        this.validateInput = validateInput;
        return this;
    }

    /**
     * Sets the usernameAttribute.
     * @param usernameAttribute The attribute in which this username will be stored.
     * @return the attribute.
     */
    public ValidatedUsernameBuilder usernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
        return this;
    }

    @Override
    public Boolean validateInput() {
        return validateInput;
    }

    @Override
    public String usernameAttribute() {
        return usernameAttribute;
    }
}
