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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import java.util.Collections;
import java.util.List;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

import com.google.auto.service.AutoService;
import com.google.inject.Singleton;

/**
 * A {@link NextGenScriptContext} for ConfigProviderNode.
 */
@Singleton
@AutoService(ScriptContext.class)
public class ConfigProviderNodeContext extends NextGenScriptContext<ConfigProviderNodeBindings> {

    /**
     * The name of the ConfigProviderNode script context.
     */
    public static final String CONFIG_PROVIDER_NEXT_GEN_NAME = "CONFIG_PROVIDER_NODE_NEXT_GEN";

    /**
     * Constructs a new {@link ConfigProviderNodeContext}.
     */
    public ConfigProviderNodeContext() {
        super(ConfigProviderNodeBindings.class);
    }

    @Override
    protected Script getDefaultScript() {
        return Script.EMPTY_SCRIPT;
    }

    @Override
    protected List<String> getContextWhiteList() {
        return List.of();
    }

    @Override
    public ConfigProviderNodeBindings getExampleBindings() {
        return ConfigProviderNodeBindings.builder()
                .withNodeState(null)
                .withIdRepo(null)
                .withSecrets(null)
                .withHeaders(Collections.emptyMap())
                .withHttpClient(null)
                .withQueryParameters(Collections.emptyMap())
                .withExistingSession(Collections.emptyMap())
                .withScriptedIdentityRepository(null)
                .build();
    }

    @Override
    public String name() {
        return CONFIG_PROVIDER_NEXT_GEN_NAME;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-05";
    }
}
