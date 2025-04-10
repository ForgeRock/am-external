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
package org.forgerock.openam.auth.node.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.script.Bindings;
import javax.validation.constraints.NotNull;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.openam.scripting.domain.Script;

/**
 * Utilities for running scripts in authentication nodes.
 */
public interface AuthScriptUtilities {
    /**
     * Node state identifier for scripts bindings.
     */
    String STATE_IDENTIFIER = "nodeState";
    /**
     * HTTP client identifier for scripts bindings.
     */
    String HTTP_CLIENT_IDENTIFIER = "httpClient";
    /**
     * Request parameter identifier for script bindings.
     */
    String QUERY_PARAMETER_IDENTIFIER = "requestParameters";
    /**
     * ID Repository identifier for script bindings.
     */
    String ID_REPO_IDENTIFIER = "idRepository";
    /**
     * Secrets identifier for script bindings.
     */
    String SECRETS_IDENTIFIER = "secrets";
    /**
     * Audit entry detail identifier for script bindings.
     */
    String AUDIT_ENTRY_DETAIL = "auditEntryDetail";
    /**
     * Request headers identifier for script bindings.
     */
    String HEADERS_IDENTIFIER = "requestHeaders";
    /**
     * Existing session identifier for script bindings.
     */
    String EXISTING_SESSION = "existingSession";
    /**
     * Callbacks identifier for script bindings.
     */
    String CALLBACKS_IDENTIFIER = "callbacks";
    /**
     * Node state wildcard character.
     */
    String WILDCARD = "*";
    /**
     * Action object identifier for script bindings.
     */
    String ACTION = "action";
    /**
     * Outcome object identifier for script bindings.
     */
    String OUTCOME_IDENTIFIER = "outcome";
    /**
     * Callbacks builder object for script bindings.
     */
    String CALLBACKS_BUILDER = "callbacksBuilder";
    /**
     * Request cookies identifier for script bindings.
     */
    String REQUEST_COOKIES_IDENTIFIER = "requestCookies";
    /**
     * SAML2 Auth Request binding.
     */
    String SAML_APPLICATION = "samlApplication";
    /**
     * OAuth Request binding.
     */
    String OAUTH_APPLICATION = "oauthApplication";

    /**
     * The request headers are unmodifiable, this prevents them being converted into javascript. This method
     * iterates the underlying collections, adding the values to modifiable collections.
     *
     * @param input the headers, must not be null
     * @return the headers in modifiable collections
     */
    Map<String, List<String>> convertHeadersToModifiableObjects(@NotNull ListMultimap<String, String> input);

    /**
     * Get a HTTP client that can be used within a script.
     *
     * @param script the script that will use the client, must not be null
     * @return the HTTP client
     */
    ChfHttpClient getLegacyHttpClient(@NotNull Script script);

    /**
     * Get the session properties for the session identified by the SSO token.
     *
     * @param ssoTokenId the SSO Token session identifier
     * @return a Map of session properties
     */
    Map<String, String> getSessionProperties(String ssoTokenId);

    /**
     * The request parameters are unmodifiable, this prevents them being converted into javascript. This method
     * copies unmodifiable to modifiable collections.
     *
     * @param input the parameters, not null
     * @return the parameters in modifiable collections
     */
    Map<String, List<String>> convertParametersToModifiableObjects(@NotNull Map<String, List<String>> input);

    /**
     * Get the contents of the audit entry details binding and encapsulate within a JSON object.
     *
     * @param bindings the script bindings, must not be null
     * @return a {@link JsonValue} object containing an object with a single field with key "auditInfo" and value equal
     * to the audit details saved within the script
     * @throws NodeProcessException if the audit entry details binding does not contain a String or Map
     */
    JsonValue getAuditEntryDetails(@NotNull Bindings bindings) throws NodeProcessException;

    /**
     * Turn the action result into an action object.
     *
     * @param actionResult     the raw action.
     * @param evalVersion      the evaluator version of the script.
     * @param callbacksBuilder the raw builder for callbacks.
     * @return an optional action, or empty optional if no action is found.
     */
    Optional<Action> getAction(Object actionResult, EvaluatorVersion evalVersion,
            Object callbacksBuilder);

    /**
     * Get the outcome string.
     *
     * @param rawOutcome      the raw outcome from the script.
     * @param allowedOutcomes outcomes that are allowed by the node.
     * @return the outcome string.
     * @throws NodeProcessException if the outcome is missing or is an invalid type.
     */
    String getOutcome(Object rawOutcome, List<String> allowedOutcomes) throws NodeProcessException;

    /**
     * If the context request params include {@value DecisionNodeApplicationConstants#SAML_OBJECT_KEY_PARAM},
     * then will retrieve the serialised SAML application object from CTS, deserialise it and return it.
     *
     * @param context Context object that specifies the required params.
     * @return SAML Application object if found in CTS, otherwise <pre>Optional.empty()</pre>
     */
    Optional<SAMLScriptedBindingObject> getSamlDecisionNodeApplication(TreeContext context) throws NodeProcessException;

    /**
     * If the context request params include {@value DecisionNodeApplicationConstants#OAUTH_OBJECT_KEY_PARAM},
     * then will retrieve the serialised OAuth application object from CTS, deserialise it and return it.
     *
     * @param context Context object that specifies the required params.
     * @return OAuth Application object if found in CTS, otherwise <pre>Optional.empty()</pre>
     */
    Optional<OAuthScriptedBindingObject> getOauthDecisionNodeApplication(TreeContext context)
            throws NodeProcessException;
}
