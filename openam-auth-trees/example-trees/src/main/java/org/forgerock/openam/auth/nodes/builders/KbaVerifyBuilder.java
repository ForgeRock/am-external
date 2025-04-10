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

import org.forgerock.openam.auth.nodes.KbaVerifyNode;

/**
 * A builder for the KbaVerifyNode.
 */
public class KbaVerifyBuilder extends AbstractNodeBuilder implements KbaVerifyNode.Config {
    private static final String DEFAULT_DISPLAY_NAME = "Create KBA Q&A";
    private String identityAttribute;
    private String kbaInfoAttribute;

    /**
     * Construct a KbaVerifyNode.
     */
    public KbaVerifyBuilder() {
        super(DEFAULT_DISPLAY_NAME, KbaVerifyNode.class);
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }

    /**
     * Set the identity attribute for this node.
     *
     * @param identityAttribute the identity attribute
     * @return this builder
     */
    public KbaVerifyBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    @Override
    public String kbaInfoAttribute() {
        return kbaInfoAttribute;
    }

    /**
     * Set the KBA info attribute for this node.
     *
     * @param kbaInfoAttribute the KBA info attribute
     * @return this builder
     */
    public KbaVerifyBuilder kbaInfoAttribute(String kbaInfoAttribute) {
        this.kbaInfoAttribute = kbaInfoAttribute;
        return this;
    }
}
