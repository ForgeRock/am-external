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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.service;

import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_SP_ADAPTER;

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScript;

/**
 * Default global script configurations for SAML2 scripts.
 */
public enum Saml2GlobalScript implements GlobalScript {

    SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT("SAML2 IDP Attribute Mapper Script",
            "c4f22465-2368-4e27-8013-e6399974fd48", SAML2_IDP_ATTRIBUTE_MAPPER),
    SAML2_IDP_ADAPTER_SCRIPT("SAML2 IDP Adapter Script",
            "248b8a56-df81-4b1b-b4ba-45d994f6504c", SAML2_IDP_ADAPTER),
    SAML2_SP_ADAPTER_SCRIPT("SAML2 SP Adapter Script",
             "69f06e63-128c-4e2f-af52-079a8a6f448b", SAML2_SP_ADAPTER);

    private final String displayName;
    private final String id;
    private final ScriptContext context;

    Saml2GlobalScript(String displayName, String id, ScriptContext context) {
        this.displayName = displayName;
        this.id = id;
        this.context = context;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }
}
