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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;

/**
 * A wrapper class to simplify using Response objects in scripts.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class ResponseScriptWrapper {

    /**
     * The response headers.
     */
    public final Map<String, List<String>> headers;
    /**
     * Whether the response was successful.
     */
    public final boolean ok;
    /**
     * The status of the response.
     */
    public final int status;
    /**
     * The status text of the response, e.g. OK for 200.
     */
    public final String statusText;
    private final Response response;

    /**
     * Constructs the ResponseScriptWrapper.
     *
     * @param response The response to wrap.
     */
    ResponseScriptWrapper(Response response) {
        this.response = response;
        this.headers = response.getHeaders().copyAsMultiMapOfStrings();
        this.ok = response.getStatus().isSuccessful();
        this.status = response.getStatus().getCode();
        this.statusText = response.getStatus().getReasonPhrase();
    }

    /**
     * Returns a copy of the "application/x-www-form-urlencoded" entity decoded as a form.
     *
     * @return The "application/x-www-form-urlencoded" entity as a form.
     * @throws IOException if the entity cannot be read entirely as a string.
     */
    public Map<String, List<String>> formData() throws IOException {
        return new HashMap<>(response.getEntity().getForm());
    }

    /**
     * Returns a copy of the "application/json" entity decoded as JSON data as a Map.
     *
     * @return The "application/json" entity decoded as JSON data as a Map.
     * @throws IOException if the entity cannot be read entirely as JSON.
     */
    public Map<String, Object> json() throws IOException {
        return JsonValue.json(response.getEntity().getJson()).asMap();
    }

    /**
     * Returns the content of this entity decoded as a string.
     *
     * @return The content of this entity decoded as a string (never {@code null}).
     * @throws IOException If an IO error occurred while reading the content.
     */
    public String text() throws IOException {
        return response.getEntity().getString();
    }

}
