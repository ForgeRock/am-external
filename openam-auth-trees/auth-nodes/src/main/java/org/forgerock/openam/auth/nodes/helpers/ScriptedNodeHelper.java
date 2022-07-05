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
package org.forgerock.openam.auth.nodes.helpers;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.validation.constraints.NotNull;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.session.Session;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;

/**
 * Utilities for running scripts in authentication nodes.
 */
public final class ScriptedNodeHelper {

    private static final Logger logger = LoggerFactory.getLogger(ScriptedNodeHelper.class);

    private ScriptedNodeHelper() {
    }

    /**
     * Shared state identifier for scripts bindings.
     *
     * @deprecated Use {@link #STATE_IDENTIFIER} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated
    public static final String SHARED_STATE_IDENTIFIER = "sharedState";

    /**
     * Transient state identifier for scripts bindings.
     *
     * @deprecated Use {@link #STATE_IDENTIFIER} instead as this method does not leak implementation detail
     * of the specific type of state.
     */
    @Deprecated
    public static final String TRANSIENT_STATE_IDENTIFIER = "transientState";

    /**
     * Node state identifier for scripts bindings.
     */
    public static final String STATE_IDENTIFIER = "nodeState";

    /**
     * HTTP client identifier for scripts bindings.
     */
    public static final String HTTP_CLIENT_IDENTIFIER = "httpClient";

    /**
     * Logger identifier for script bindings.
     */
    public static final String LOGGER_VARIABLE_NAME = "logger";

    /**
     * Realm identifier for script bindings.
     */
    public static final String REALM_IDENTIFIER = "realm";

    /**
     * Request parameter identifier for script bindings.
     */
    public static final String QUERY_PARAMETER_IDENTIFIER = "requestParameters";

    /**
     * ID Repository identifier for script bindings.
     */
    public static final String ID_REPO_IDENTIFIER = "idRepository";

    /**
     * Secrets identifier for script bindings.
     */
    public static final String SECRETS_IDENTIFIER = "secrets";

    /**
     * Audit entry detail identifier for script bindings.
     */
    public static final String AUDIT_ENTRY_DETAIL = "auditEntryDetail";

    /**
     * Request headers identifier for script bindings.
     */
    public static final String HEADERS_IDENTIFIER = "requestHeaders";

    /**
     * Existing session identifier for script bindings.
     */
    public static final String EXISTING_SESSION = "existingSession";

    /**
     * Callbacks identifier for script bindings.
     */
    public static final String CALLBACKS_IDENTIFIER = "callbacks";

    /**
     * The request headers are unmodifiable, this prevents them being converted into javascript. This method
     * iterates the underlying collections, adding the values to modifiable collections.
     *
     * @param input the headers, must not be null
     * @return the headers in modifiable collections
     */
    public static Map<String, List<String>> convertHeadersToModifiableObjects(
            @NotNull ListMultimap<String, String> input) {
        Reject.ifNull(input);
        return input.keySet().stream()
                .map(key -> Map.entry(key, input.get(key)))
                .collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue()),
                        (oldEntry, newEntry) -> newEntry,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                ));
    }

    /**
     * Get a HTTP client that can be used within a script.
     *
     * @param script the script that will use the client, must not be null
     * @param httpClientFactory the HTTP client factory, must not be null
     *
     * @return the HTTP client
     */
    public static ChfHttpClient getHttpClient(@NotNull Script script,
            @NotNull ScriptHttpClientFactory httpClientFactory) {
        Reject.ifNull(script, httpClientFactory);
        ScriptingLanguage scriptType = script.getLanguage();
        if (scriptType == null) {
            return null;
        }
        return httpClientFactory.getScriptHttpClient(scriptType);
    }

    /**
     * Get the session properties for the session identified by the SSO token.
     *
     * @param sessionService the SessionService, must not be null
     * @param ssoTokenId the SSO Token session identifier
     * @return a Map of session properties
     */
    public static Map<String, String> getSessionProperties(@NotNull SessionService sessionService, String ssoTokenId) {
        Reject.ifNull(sessionService);
        Map<String, String> properties = null;
        try {
            Session session = sessionService.getSession(new SessionID(ssoTokenId));
            if (session != null) {
                properties = new HashMap<>(session.getProperties());
            }
        } catch (SessionException e) {
            logger.error("Failed to get existing session", e);
        }
        return properties;
    }

    /**
     * The request parameters are unmodifiable, this prevents them being converted into javascript. This method
     * copies unmodifiable to modifiable collections.
     *
     * @param input the parameters, not null
     * @return the parameters in modifiable collections
     */
    public static Map<String, List<String>> convertParametersToModifiableObjects(
            @NotNull Map<String, List<String>> input) {
        Reject.ifNull(input);
        return input.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new ArrayList<>(entry.getValue()),
                    (oldEntry, newEntry) -> newEntry,
                    () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                ));
    }

    /**
     * Get the contents of the audit entry details binding and encapsulate within a JSON object.
     *
     * @param bindings the script bindings, must not be null
     * @return a {@link JsonValue} object containing an object with a single field with key "auditInfo" and value equal
     * to the audit details saved within the script
     * @throws NodeProcessException if the audit entry details binding does not contain a String or Map
     */
    public static JsonValue getAuditEntryDetails(@NotNull Bindings bindings) throws NodeProcessException {
        Object rawAuditEntryDetail = bindings.get(AUDIT_ENTRY_DETAIL);
        if (rawAuditEntryDetail != null) {
            if (rawAuditEntryDetail instanceof String || rawAuditEntryDetail instanceof Map) {
                return json(object(field("auditInfo", rawAuditEntryDetail)));
            } else {
                logger.warn("script auditEntryDetail not type String or Map");
                throw new NodeProcessException("Invalid auditEntryDetail type from script");
            }
        }
        return null;
    }
}
