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
 * Copyright 2024-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

/**
 * A {@link NextGenScriptContext} for ConfigProviderNode.
 */
@Singleton
public class ConfigProviderNodeContext extends NextGenScriptContext<ConfigProviderNodeBindings> {

    /**
     * The name of the ConfigProviderNode script context.
     */
    public static final String CONFIG_PROVIDER_NEXT_GEN_NAME = "CONFIG_PROVIDER_NODE_NEXT_GEN";
    private final SAMLScriptedBindingObject exampleSamlObject;
    private final OAuthScriptedBindingObject exampleOauthObject;

    /**
     * Constructs a new {@link ConfigProviderNodeContext}.
     */
    @Inject
    ConfigProviderNodeContext(@SAMLScriptedBindingObject.ExampleObject SAMLScriptedBindingObject exampleSamlObject,
            @OAuthScriptedBindingObject.ExampleObject OAuthScriptedBindingObject exampleOauthObject) {
        super(ConfigProviderNodeBindings.class);
        this.exampleSamlObject = exampleSamlObject;
        this.exampleOauthObject = exampleOauthObject;
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
                .withIdRepo(null)
                .withSecrets(null)
                .withHttpClient(null)
                .withNodeState(null)
                .withCallbacks(Collections.emptyList())
                .withHeaders(Collections.emptyMap())
                .withQueryParameters(Collections.emptyMap())
                .withScriptedIdentityRepository(null)
                .withResumedFromSuspend(false)
                .withExistingSession(Collections.emptyMap())
                .withRequestCookies(Collections.emptyMap())
                .withTree(null)
                .withSamlApplication(exampleSamlObject)
                .withOauthApplication(exampleOauthObject)
                .withScriptedLocalizedMessage(null)
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
