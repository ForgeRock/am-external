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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;

import com.google.auto.service.AutoService;

/**
 * Definition of {@link ScriptContext} for the OIDC node.
 */
@Singleton
@AutoService(ScriptContext.class)
public class OidcNodeScriptContext extends NextGenScriptContext<OidcNodeBindings> {

    /**
     * The compile time constant for the name of the OIDC node script context.
     */
    public static final String OIDC_NODE_NAME = "OIDC_NODE";

    @Inject
    OidcNodeScriptContext() {
        super(OidcNodeBindings.class);
    }

    @Override
    public String name() {
        return OIDC_NODE_NAME;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-20";
    }

    @Override
    public OidcNodeBindings getExampleBindings() {
        return OidcNodeBindings.builder()
                .withJwtClaims(json(object()))
                .withNodeState(null)
                .withHeaders(Map.of())
                .withExistingSession(Map.of())
                .build();
    }

    @Override
    public Script getDefaultScript() {
        return Script.EMPTY_SCRIPT;
    }

    @Override
    protected List<String> getContextWhiteList() {
        return List.of();
    }
}
