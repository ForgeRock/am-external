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
package org.forgerock.openam.auth.nodes.helpers;

import static java.util.Objects.requireNonNull;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.OAUTH_OBJECT_KEY_PARAM;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.SAML_OBJECT_KEY_PARAM;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.Bindings;
import javax.validation.constraints.NotNull;

import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.application.tree.OAuthScriptedBindingObjectAdapter;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.ActionWrapper;
import org.forgerock.openam.auth.nodes.script.ScriptedCallbacksBuilder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.session.Session;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.saml2.common.SAML2FailoverUtils;

/**
 * Utilities for running scripts in authentication nodes.
 */
@Singleton
public class ScriptedNodeHelper implements AuthScriptUtilities {

    private static final Logger logger = LoggerFactory.getLogger(ScriptedNodeHelper.class);

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

    private final ScriptHttpClientFactory httpClientFactory;
    private final Provider<SessionService> sessionServiceProvider;
    private final CTSPersistentStore cts;
    private final OAuthScriptedBindingObjectAdapter scriptedBindingAdapter;

    /**
     * Constructs a new instance of the helper.
     * @param httpClientFactory      The factory for creating HTTP clients.
     * @param sessionServiceProvider A provider for the service for managing sessions.
     * @param cts                    The core token store.
     * @param scriptedBindingAdapter The adapter for converting between {@link OAuthScriptedBindingObject}
     *                               and {@link org.forgerock.am.cts.api.tokens.Token}.
     */
    @Inject
    public ScriptedNodeHelper(ScriptHttpClientFactory httpClientFactory,
            Provider<SessionService> sessionServiceProvider, CTSPersistentStore cts,
            OAuthScriptedBindingObjectAdapter scriptedBindingAdapter) {
        this.httpClientFactory = requireNonNull(httpClientFactory);
        this.sessionServiceProvider = requireNonNull(sessionServiceProvider);
        this.cts = cts;
        this.scriptedBindingAdapter = scriptedBindingAdapter;
    }

    @Override
    public Map<String, List<String>> convertHeadersToModifiableObjects(
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

    @Override
    public ChfHttpClient getLegacyHttpClient(@NotNull Script script) {
        Reject.ifNull(script);
        ScriptingLanguage scriptType = script.getLanguage();
        if (scriptType == null) {
            return null;
        }
        return httpClientFactory.getScriptHttpClient(scriptType);
    }

    @Override
    public Map<String, String> getSessionProperties(String ssoTokenId) {
        if (isEmpty(ssoTokenId)) {
            return null;
        }
        Map<String, String> properties = null;
        try {
            SessionService sessionService = sessionServiceProvider.get();
            Session session = sessionService.getSession(sessionService.asSessionID(ssoTokenId));
            if (session != null) {
                properties = new HashMap<>(session.getProperties());
            }
        } catch (SessionException e) {
            logger.error("Failed to get existing session", e);
        }
        return properties;
    }

    @Override
    public Map<String, List<String>> convertParametersToModifiableObjects(
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

    @Override
    public JsonValue getAuditEntryDetails(@NotNull Bindings bindings) throws NodeProcessException {
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

    @Override
    public Optional<Action> getAction(Object actionResult, EvaluatorVersion evalVersion,
            Object callbacksBuilder) {
        Optional<Action> action = Optional.empty();
        switch (evalVersion) {
        case V1_0:
            if (actionResult instanceof Action) {
                action = Optional.of((Action) actionResult);
            } else {
                logger.warn("Found an action result from scripted node, but it was not an Action object");
            }
            break;

        case V2_0:
            if (actionResult instanceof ActionWrapper) {
                ActionWrapper actionWrapper = ((ActionWrapper) actionResult);
                if (callbacksBuilder instanceof ScriptedCallbacksBuilder) {
                    actionWrapper.setCallbacks(((ScriptedCallbacksBuilder) callbacksBuilder).getCallbacks());
                }
                action = actionWrapper.isEmpty() ? Optional.empty() : Optional.of(actionWrapper.buildAction());
            } else {
                logger.warn("Found an action result from scripted node, but it was not an ActionWrapper object");
            }
            break;

        default:
            logger.error("Unexpected script evaluator version");
            break;
        }
        return action;
    }

    @Override
    public String getOutcome(Object rawOutcome, List<String> allowedOutcomes) throws NodeProcessException {
        if (rawOutcome instanceof CharSequence) {
            rawOutcome = rawOutcome.toString();
        }
        if (!(rawOutcome instanceof String outcome)) {
            logger.warn("script outcome error");
            throw new NodeProcessException("Script must set '" + OUTCOME_IDENTIFIER + "' to a string.");
        }
        if (!allowedOutcomes.contains(outcome)) {
            logger.warn("invalid script outcome {}", outcome);
            throw new NodeProcessException("Invalid outcome from script, '" + outcome + "'");
        }
        return outcome;
    }

    /**
     * If the request params include {@value DecisionNodeApplicationConstants#SAML_OBJECT_KEY_PARAM},
     * then will retrieve SAML application object from the CTS store.
     *
     * @param context The current tree context.
     * @return SAML application object, or <pre>Optional.empty</pre> if not found.
     * @throws NodeProcessException If the SAML application object could not be retrieved.
     */
    public Optional<SAMLScriptedBindingObject> getSamlDecisionNodeApplication(TreeContext context)
            throws NodeProcessException {
        var requestParams = context.request.parameters;
        String storageKey = getFirstItem(requestParams.get(SAML_OBJECT_KEY_PARAM));
        if (isBlank(storageKey)) {
            return Optional.empty();
        }
        try {
            SAMLScriptedBindingObject application =
                    (SAMLScriptedBindingObject) SAML2FailoverUtils.retrieveSAML2Token(storageKey);
            if  (application == null) {
                throw new NodeProcessException("Failed to retrieve SAML application object from CTS store");
            } else {
                return Optional.of(application);
            }
        } catch (SAML2TokenRepositoryException e) {
            throw new NodeProcessException("Failed to retrieve SAML application object from CTS store", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<OAuthScriptedBindingObject> getOauthDecisionNodeApplication(TreeContext context)
            throws NodeProcessException {
        var requestParams = context.request.parameters;
        String storageKey = getFirstItem(requestParams.get(OAUTH_OBJECT_KEY_PARAM));
        if (isBlank(storageKey)) {
            return Optional.empty();
        }
        try {
            Token token = cts.read(storageKey);
            if (token == null) {
                throw new NodeProcessException("Failed to retrieve OAuth application object from CTS store");
            }
            OAuthScriptedBindingObject application = scriptedBindingAdapter.fromToken(token);
            if  (application == null) {
                throw new NodeProcessException("Failed to retrieve OAuth application object from CTS store");
            } else {
                return Optional.of(application);
            }
        } catch (CoreTokenException e) {
            throw new NodeProcessException("Failed to retrieve OAuth application object from CTS store", e);
        }
    }
}
