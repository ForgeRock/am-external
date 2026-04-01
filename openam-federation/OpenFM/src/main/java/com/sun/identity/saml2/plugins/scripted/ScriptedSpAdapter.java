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
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_SP_ADAPTER;

import java.io.PrintWriter;

import javax.script.ScriptException;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.plugins.SPAdapter;
import org.forgerock.openam.saml2.service.Saml2SpAdapterContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlBindings.SpBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAdapterScriptBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPostSingleSignOnFailureBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPostSingleSignOnSuccessBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPreSingleSignOnProcessBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPreSingleSignOnRequestBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpUserIdLoginLogoutBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpUserIdRequestResponseBindings;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.Response;

/**
 * Scripted implementation of the {@link SPAdapter}
 */
public class ScriptedSpAdapter implements SPAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ScriptedSpAdapter.class);

    private final ScriptEvaluationHelper<SamlSpAdapterScriptBindings> nextGenScriptEvaluationHelper;
    private final ScriptEvaluationHelper<LegacyScriptBindings> legacyScriptEvaluationHelper;

    private final Client httpClient;
    private final ScriptExecutor executor;
    private final RealmLookup realmLookup;

    @Inject
    public ScriptedSpAdapter(
            ScriptEvaluatorFactory scriptEvaluatorFactory,
            Saml2SpAdapterContext saml2SpAdapterContext,
            Client httpClient,
            ScriptExecutor executor,
            RealmLookup realmLookup
    ) {
        this.legacyScriptEvaluationHelper = scriptEvaluatorFactory
                .create(SAML2_SP_ADAPTER)
                .getScriptEvaluationHelper();
        this.nextGenScriptEvaluationHelper = scriptEvaluatorFactory
                .create(saml2SpAdapterContext)
                .getScriptEvaluationHelper();
        this.realmLookup = realmLookup;
        this.httpClient = httpClient;
        this.executor = executor;
    }

    @Override
    public void preSingleSignOnRequest(
            String hostedEntityId, String idpEntityId, String realm,
            HttpServletRequest request, HttpServletResponse response, AuthnRequest authnRequest
    ) throws SAML2Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
        var methodName = "preSingleSignOnRequest";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlSpPreSingleSignOnRequestBindings bindings =
                    SpBindings.preSingleSignOnRequest()
                            .withAuthnRequest(authnRequest)
                            .withIdpEntityId(idpEntityId)
                            .withHostedEntityId(hostedEntityId)
                            .withHttpClient(httpClient)
                            .withRequest(request)
                            .withResponse(response)
                            .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                            .build();

            try {
                legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlSpAdapterScriptBindings bindings =
                    SamlSpAdapterScriptBindings.builder()
                            .withRequest(request)
                            .withResponse(response)
                            .withAuthnRequest(authnRequest)
                            .withIdpEntityId(idpEntityId)
                            .withHostedEntityId(hostedEntityId)
                            .build();

            try {
                nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
    }

    @Override
    public void preSingleSignOnProcess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response,
            AuthnRequest authnRequest,
            Response ssoResponse,
            String profile
    ) throws SAML2Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
        var methodName = "preSingleSignOnProcess";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlSpPreSingleSignOnProcessBindings bindings =
                    SpBindings.preSingleSignOnProcess()
                            .withAuthnRequest(authnRequest)
                            .withSsoResponse(ssoResponse)
                            .withProfile(profile)
                            .withHostedEntityId(hostedEntityId)
                            .withHttpClient(httpClient)
                            .withRequest(request)
                            .withResponse(response)
                            .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                            .build();
            try {
                legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }

        }
        case V2_0 -> {
            SamlSpAdapterScriptBindings bindings =
                    SamlSpAdapterScriptBindings.builder()
                            .withRequest(request)
                            .withResponse(response)
                            .withAuthnRequest(authnRequest)
                            .withSsoResponse(ssoResponse)
                            .withProfile(profile)
                            .withHostedEntityId(hostedEntityId)
                            .build();
            try {
                nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
    }

    @Override
    public boolean postSingleSignOnSuccess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, PrintWriter out, Object ssoToken, AuthnRequest authnRequest,
            Response ssoResponse, String profile, boolean isFederation) throws SAML2Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
        String methodName = "postSingleSignOnSuccess";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlSpPostSingleSignOnSuccessBindings bindings =
                    SpBindings.postSingleSignOnSuccess()
                            .withAuthnRequest(authnRequest)
                            .withProfile(profile)
                            .withOut(out)
                            .withSsoResponse(ssoResponse)
                            .withSession(ssoToken)
                            .withFederation(isFederation)
                            .withHostedEntityId(hostedEntityId)
                            .withHttpClient(httpClient)
                            .withRequest(request)
                            .withResponse(response)
                            .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                            .build();
            try {
                return legacyScriptEvaluationHelper.evaluateFunction(
                        script, bindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }

        }
        case V2_0 -> {
            SamlSpAdapterScriptBindings bindings =
                    SamlSpAdapterScriptBindings.builder()
                            .withRequest(request)
                            .withResponse(response)
                            .withAuthnRequest(authnRequest)
                            .withSsoResponse(ssoResponse)
                            .withSsoToken(ssoToken)
                            .withProfile(profile)
                            .withFederation(isFederation)
                            .withHostedEntityId(hostedEntityId)
                            .build();
            try {
                return nextGenScriptEvaluationHelper.evaluateFunction(
                        script, bindings, Boolean.class, methodName, getRealm(realm)
                ).orElse(false);
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }
        return false;
    }

    @Override
    public boolean postSingleSignOnFailure(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, Response ssoResponse,
            String profile, int failureCode) {
        try {
            Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
            String methodName = "postSingleSignOnFailure";
            switch (script.getEvaluatorVersion()) {
            case V1_0 -> {
                SamlSpPostSingleSignOnFailureBindings bindings = SpBindings.postSingleSignOnFailure()
                        .withAuthnRequest(authnRequest)
                        .withProfile(profile)
                        .withSsoResponse(ssoResponse)
                        .withFailureCode(failureCode)
                        .withHostedEntityId(hostedEntityId)
                        .withHttpClient(httpClient)
                        .withRequest(request)
                        .withResponse(response)
                        .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                        .build();
                try {
                    return legacyScriptEvaluationHelper.evaluateFunction(
                            script, bindings, Boolean.class, methodName, getRealm(realm)
                    ).orElse(false);
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            case V2_0 -> {
                SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                        .withRequest(request)
                        .withResponse(response)
                        .withAuthnRequest(authnRequest)
                        .withProfile(profile)
                        .withSsoResponse(ssoResponse)
                        .withFailureCode(failureCode)
                        .withHostedEntityId(hostedEntityId)
                        .build();
                try {
                    return nextGenScriptEvaluationHelper.evaluateFunction(
                            script, bindings, Boolean.class, methodName, getRealm(realm)
                    ).orElse(false);
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            }
        } catch (SAML2Exception e) {
            logger.error("Exception in SP Adapter's postSingleSignOnFailure", e);
        }
        return false;
    }

    @Override
    public void postNewNameIDSuccess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, String userId, ManageNameIDRequest idRequest, ManageNameIDResponse idResponse,
            String binding) {
        try {
            Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
            var methodName = "postNewNameIDSuccess";
            switch (script.getEvaluatorVersion()) {
            case V1_0 -> {
                SamlSpUserIdRequestResponseBindings bindings =
                        SpBindings.userIdRequestBindings()
                                .withUserId(userId)
                                .withIdRequest(idRequest)
                                .withIdResponse(idResponse)
                                .withBinding(binding)
                                .withHostedEntityId(hostedEntityId)
                                .withHttpClient(httpClient)
                                .withRequest(request)
                                .withResponse(response)
                                .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                                .build();
                try {
                    legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            case V2_0 -> {
                SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                        .withRequest(request)
                        .withResponse(response)
                        .withUserId(userId)
                        .withBinding(binding)
                        .withHostedEntityId(hostedEntityId)
                        .build();
                try {
                    nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            }

        } catch (SAML2Exception e) {
            logger.error("Exception in SP Adapter's postNewNameIDSuccess", e);
        }
    }

    @Override
    public void postTerminateNameIDSuccess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, String userId, ManageNameIDRequest idRequest,
            ManageNameIDResponse idResponse, String binding) {
        try {
            Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
            var methodName = "postTerminateNameIDSuccess";
            switch (script.getEvaluatorVersion()) {
            case V1_0 -> {
                SamlSpUserIdRequestResponseBindings bindings = SpBindings.userIdRequestBindings()
                        .withUserId(userId)
                        .withIdRequest(idRequest)
                        .withIdResponse(idResponse)
                        .withBinding(binding)
                        .withHostedEntityId(hostedEntityId)
                        .withHttpClient(httpClient)
                        .withRequest(request)
                        .withResponse(response)
                        .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                        .build();

                try {
                    legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            case V2_0 -> {
                SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                        .withRequest(request)
                        .withResponse(response)
                        .withUserId(userId)
                        .withBinding(binding)
                        .withHostedEntityId(hostedEntityId)
                        .build();
                try {
                    nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            }
        } catch (SAML2Exception e) {
            logger.error("Exception in SP Adapter's postTerminateNameIDSuccess", e);
        }
    }

    @Override
    public void preSingleLogoutProcess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, String userId, LogoutRequest logoutRequest,
            LogoutResponse logoutResponse, String binding) throws SAML2Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
        var methodName = "preSingleLogoutProcess";
        switch (script.getEvaluatorVersion()) {
        case V1_0 -> {
            SamlSpUserIdLoginLogoutBindings bindings =
                    SpBindings.loginLogoutBindings()
                            .withUserId(userId)
                            .withLogoutRequest(logoutRequest)
                            .withLogoutResponse(logoutResponse)
                            .withBinding(binding)
                            .withHostedEntityId(hostedEntityId)
                            .withHttpClient(httpClient)
                            .withRequest(request)
                            .withResponse(response)
                            .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                            .build();
            try {
                legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        case V2_0 -> {
            SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                    .withRequest(request)
                    .withResponse(response)
                    .withUserId(userId)
                    .withBinding(binding)
                    .withHostedEntityId(hostedEntityId)
                    .build();
            try {
                nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
            } catch (ScriptException ex) {
                throw new SAML2Exception(ex);
            }
        }
        }

    }

    @Override
    public void postSingleLogoutSuccess(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, String userId, LogoutRequest logoutRequest,
            LogoutResponse logoutResponse, String binding) {
        try {
            Script script = executor.getScriptConfiguration(realm, hostedEntityId, SP_ROLE, SP_ADAPTER_SCRIPT);
            var methodName = "postSingleLogoutSuccess";
            switch (script.getEvaluatorVersion()) {
            case V1_0 -> {
                SamlSpUserIdLoginLogoutBindings bindings = SpBindings.loginLogoutBindings()
                        .withUserId(userId)
                        .withLogoutRequest(logoutRequest)
                        .withLogoutResponse(logoutResponse)
                        .withBinding(binding)
                        .withHostedEntityId(hostedEntityId)
                        .withHttpClient(httpClient)
                        .withRequest(request)
                        .withResponse(response)
                        .withSpAdapterScriptHelper(createSpAdapterScriptHelper())
                        .build();
                try {
                    legacyScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            case V2_0 -> {
                SamlSpAdapterScriptBindings bindings = SamlSpAdapterScriptBindings.builder()
                        .withRequest(request)
                        .withResponse(response)
                        .withUserId(userId)
                        .withBinding(binding)
                        .withHostedEntityId(hostedEntityId)
                        .build();
                try {
                    nextGenScriptEvaluationHelper.evaluateFunction(script, bindings, methodName, getRealm(realm));
                } catch (ScriptException ex) {
                    throw new SAML2Exception(ex);
                }
            }
            }

        } catch (SAML2Exception e) {
            logger.error("Exception in SP Adapter's postSingleLogoutSuccess", e);
        }
    }

    private Realm getRealm(String realmName) {
        try {
            return realmLookup.lookup(realmName);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realmName, e);
        }
    }

    private static SpAdapterScriptHelper createSpAdapterScriptHelper() {
        return new SpAdapterScriptHelper();
    }
}
