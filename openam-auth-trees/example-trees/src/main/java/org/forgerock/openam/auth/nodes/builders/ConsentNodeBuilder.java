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

import org.forgerock.openam.auth.nodes.ConsentNode;

/**
 * A builder for the ConsentNode.
 */
public class ConsentNodeBuilder extends AbstractNodeBuilder implements ConsentNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Consent to Sharing";
    private boolean allRequired;

    /**
     * Construct a ConsentNode.
     */
    public ConsentNodeBuilder() {
        super(DEFAULT_DISPLAY_NAME, ConsentNode.class);
    }

    /**
     * Set whether all mappings require consent to progress.
     * @param allRequired true iff all mappings must be consented
     * @return this builder
     */
    public ConsentNodeBuilder allRequired(boolean allRequired) {
        this.allRequired = allRequired;
        return this;
    }

    @Override
    public Boolean allRequired() {
        return this.allRequired;
    }
}
