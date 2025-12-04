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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.scripting.api.http;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * Retrieve a new http client suitable for use in the scripting execution environment.
 */
@Singleton
public class ScriptHttpClientFactory {
    public static final String SCRIPTING_HTTP_CLIENT_NAME = "ScriptingHttpClient";

    /**
     * Retrieve an HTTP client appropriate for use in the scripting engine with the supplied scripting language.
     *
     * @param scriptType Script type the http client will be for
     * @return a new http client, ready for use.
     */
    public ChfHttpClient getScriptHttpClient(ScriptingLanguage scriptType) {
        return InjectorHolder.getInstance(Key.get(ChfHttpClient.class, Names.named(scriptType.name())));
    }

}
