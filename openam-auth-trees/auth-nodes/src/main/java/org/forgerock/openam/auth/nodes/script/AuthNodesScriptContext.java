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
package org.forgerock.openam.auth.nodes.script;

import org.forgerock.openam.scripting.domain.ScriptContext;

/**
 * Definitions of {@link ScriptContext}s for authentication nodes scripts.
 */
public enum AuthNodesScriptContext implements ScriptContext {

    /**
     * The default authentication tree decision node script context.
     */
    AUTHENTICATION_TREE_DECISION_NODE,
    /**
     * The default config provider node script context.
     */
    CONFIG_PROVIDER_NODE;

    /**
     * Compile-time constants to reference this context with.
     */
    public static final String AUTHENTICATION_TREE_DECISION_NODE_NAME = "AUTHENTICATION_TREE_DECISION_NODE";

    /**
     * Compile-time constants to reference this context with.
     */
    public static final String CONFIG_PROVIDER_NODE_NAME = "CONFIG_PROVIDER_NODE";
}
