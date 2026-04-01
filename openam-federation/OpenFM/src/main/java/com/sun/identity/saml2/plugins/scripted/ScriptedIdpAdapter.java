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
 * Copyright 2021-2026 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;

import javax.script.ScriptException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.plugins.IDPAdapter;
import org.forgerock.openam.saml2.service.Saml2IdpAdapterContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.Script;

import jakarta.inject.Inject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlBindings.IdpBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAdapterScriptBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreAuthenticationBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSendFailureResponseBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSendResponseBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSignResponseBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSingleSignOnBindings;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Scripted implementation of the {@link IDPAdapter}
 */
public class ScriptedIdpAdapter implements IDPAdapter {

    private final ScriptEvaluationHelper<LegacyScriptBindings> legacyScriptEvaluationHelper;
    private final ScriptEvaluationHelper<SamlIdpAdapterScriptBindings> nextGenScriptEvaluationHelper;
    private final ValidationHelper validationHelper;
    private final IdpAdapterScriptHelper idpAdapterScriptHelper;
    private final Client httpClient;
    private final ScriptExecutor executor;
    private final RealmLookup realmLookup;

    @Inject
    public ScriptedIdpAdapter(ScriptEvaluatorFactory scriptEvaluatorFactory,
            Saml2IdpAdapterContext saml2IdpAdapterContext,
            ValidationHelper validationHelper,
            IdpAdapterScriptHelper idpAdapterScriptHelper,
            Client httpClient,
            ScriptExecutor executor,
            RealmLookup realmLookup) {
        this.legacyScriptEvaluationHelper = scriptEvaluatorFactory
                .create(SAML2_IDP_ADAPTER)
                .getScriptEvaluationHelper();
        this.nextGenScriptEvaluationHelper = scriptEvaluatorFactory
                .create(saml2IdpAdapterContext)
                .getScriptEvaluationHelper();
        this.validationHelper = validationHelper;
        this.idpAdapterScriptHelper = idpAdapterScriptHelper;
        this.httpClient = httpClient;
        this.executor = executor;
        this.realmLookup = realmLookup;
    }

    /**
     * This method is empty as it is not called for Scripted IDP Adapters.
     *
     * @param hostedEntityId the host entity ID
     * @param realm          realm of the hosted IDP
     */
    @SuppressWarnings("removal")
    @Override
    @Deprecated(forRemoval = true, since = "7.3.0")
    public void initialize(String hostedEntityId, String realm) {
        // Not relevant for Scripted IDP Adapter
    }

    @Override
    public boolean preSingleSignOn(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, String reqId)
            throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);
        String methodName = "preSingleSignOn";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlIdpPreSingleSignOnBindings scriptBindings = IdpBindings.preSingleSignOn()
                    .withResponse(response)
                    .withRequestId(reqId)
                    .withRequest(request)
                    .withAuthnRequest(authnRequest)
                    .withHostedEntityId(hostedEntityId)
                    .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                    .withHttpClient(httpClient)
                    .build();
            try {
                return legacyScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlIdpAdapterScriptBindings scriptBindings = SamlIdpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withResponse(response)
                    .withAuthnRequest(authnRequest)
                    .withHostedEntityId(hostedEntityId)
                    .withReqId(reqId)
                    .build();
            try {
                return nextGenScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
        return false;
    }

    @Override
    public boolean preAuthentication(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);
        String methodName = "preAuthentication";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlIdpPreAuthenticationBindings scriptBindings =
                    IdpBindings.preAuthentication()
                            .withRelayState(relayState)
                            .withSession(session)
                            .withResponse(response)
                            .withRequest(request)
                            .withRequestId(reqId)
                            .withAuthnRequest(authnRequest)
                            .withHostedEntityId(hostedEntityId)
                            .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                            .withHttpClient(httpClient)
                            .build();
            try {
                return legacyScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlIdpAdapterScriptBindings scriptBindings = SamlIdpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withResponse(response)
                    .withAuthnRequest(authnRequest)
                    .withSsoToken(session)
                    .withHostedEntityId(hostedEntityId)
                    .withReqId(reqId)
                    .withRelayState(relayState)
                    .build();
            try {
                return nextGenScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
        return false;
    }

    @Override
    public boolean preSendResponse(AuthnRequest authnRequest, String hostedEntityId, String realm,
            HttpServletRequest request, HttpServletResponse response, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);

        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);
        String methodName = "preSendResponse";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlIdpPreSendResponseBindings scriptBindings =
                    IdpBindings.preSendResponse()
                            .withRelayState(relayState)
                            .withSession(session)
                            .withResponse(response)
                            .withRequest(request)
                            .withRequestId(reqId)
                            .withAuthnRequest(authnRequest)
                            .withHostedEntityId(hostedEntityId)
                            .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                            .withHttpClient(httpClient)
                            .build();
            try {
                return legacyScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlIdpAdapterScriptBindings scriptBindings = SamlIdpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withResponse(response)
                    .withAuthnRequest(authnRequest)
                    .withSsoToken(session)
                    .withHostedEntityId(hostedEntityId)
                    .withReqId(reqId)
                    .withRelayState(relayState)
                    .build();
            try {
                return nextGenScriptEvaluationHelper.evaluateFunction(
                        script, scriptBindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
        return false;
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostedEntityId, String realm,
            HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);
        String methodName = "preSignResponse";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlIdpPreSignResponseBindings scriptBindings =
                    IdpBindings.preSignResponse()
                            .withRelayState(relayState)
                            .withSession(session)
                            .withSaml2Response(res)
                            .withRequest(request)
                            .withAuthnRequest(authnRequest)
                            .withHostedEntityId(hostedEntityId)
                            .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                            .withHttpClient(httpClient)
                            .build();
            try {
                legacyScriptEvaluationHelper.evaluateFunction(script, scriptBindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlIdpAdapterScriptBindings scriptBindings = SamlIdpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withAuthnRequest(authnRequest)
                    .withSsoResponse(res)
                    .withSsoToken(session)
                    .withHostedEntityId(hostedEntityId)
                    .withRelayState(relayState)
                    .build();
            try {
                nextGenScriptEvaluationHelper.evaluateFunction(script, scriptBindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }

    }

    @SuppressWarnings("removal")
    @Override
    public void preSendFailureResponse(HttpServletRequest request, HttpServletResponse response,
            String faultCode, String faultDetail) throws SAML2Exception {
        throw new SAML2Exception("Unsupported function for ScriptedIdpAdapter - use preSendFailureResponse(String, "
                + "String, HttpServletRequest, HttpServletResponse, String, String)");
    }

    @Override
    public void preSendFailureResponse(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, String faultCode, String faultDetail)
            throws SAML2Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);
        String methodName = "preSendFailureResponse";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlIdpPreSendFailureResponseBindings scriptBindings =
                    IdpBindings.preSendFailureResponse()
                            .withRequest(request)
                            .withResponse(response)
                            .withFaultCode(faultCode)
                            .withFaultDetail(faultDetail)
                            .withHostedEntityId(hostedEntityId)
                            .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                            .withHttpClient(httpClient)
                            .build();
            try {
                legacyScriptEvaluationHelper.evaluateFunction(script, scriptBindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlIdpAdapterScriptBindings scriptBindings = SamlIdpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withResponse(response)
                    .withHostedEntityId(hostedEntityId)
                    .withFaultCode(faultCode)
                    .withFaultDetail(faultDetail)
                    .build();
            try {
                nextGenScriptEvaluationHelper.evaluateFunction(script, scriptBindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
    }

    private void validateCommonAttributes(String hostedEntityId, String realm) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
    }

    private Realm getRealm(String realmName) {
        try {
            return realmLookup.lookup(realmName);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realmName, e);
        }
    }

}
