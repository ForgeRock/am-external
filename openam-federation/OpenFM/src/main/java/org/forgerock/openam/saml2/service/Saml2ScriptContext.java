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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.saml2.service;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT;

import org.forgerock.openam.scripting.domain.ScriptContext;

/**
 * Definitions of {@link ScriptContext}s for SAML2 scripts.
 */
public enum Saml2ScriptContext implements ScriptContext {

    /**
     * The default SAML2 IDP attribute mapper script context.
     */
    SAML2_IDP_ATTRIBUTE_MAPPER(IDP_ATTRIBUTE_MAPPER_SCRIPT),

    /**
     * The default SAML2 IDP adapter script context.
     */
    SAML2_IDP_ADAPTER(IDP_ADAPTER_SCRIPT);

    private final String attribute;

    /**
     * The constructor.
     *
     * @param attribute used to configure this script on saml entity.
     */
    Saml2ScriptContext(String attribute) {
        this.attribute = attribute;
    }

    /**
     * Gets the name of the attribute used to configure this script on saml entity.
     *
     * @return the attribute used to configure this script on saml entity.
     */
    String getAttribute() {
        return attribute;
    }
}
