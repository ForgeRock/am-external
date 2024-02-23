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
 * Copyright 2021-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.saml2.plugins.IDPAdapter;
import org.forgerock.openam.saml2.service.Saml2ScriptContext;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptBindings;

import com.google.inject.Inject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlBindings.IdpBindings;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;

/**
 * Scripted implementation of the {@link IDPAdapter}
 */
public class ScriptedIdpAdapter implements IDPAdapter {

    private final ScriptEvaluator scriptEvaluator;
    private final ValidationHelper validationHelper;
    private final IdpAdapterScriptHelper idpAdapterScriptHelper;
    private final ScriptExecutor executor;

    @Inject
    public ScriptedIdpAdapter(ScriptEvaluatorFactory scriptEvaluatorFactory,
            ValidationHelper validationHelper,
            IdpAdapterScriptHelper idpAdapterScriptHelper,
            ScriptExecutor executor) {
        this.scriptEvaluator = scriptEvaluatorFactory.create(SAML2_IDP_ADAPTER);
        this.validationHelper = validationHelper;
        this.idpAdapterScriptHelper = idpAdapterScriptHelper;
        this.executor = executor;
    }

    /**
     * This initialise method is empty as it is not called for Scripted IDP Adapters.
     *
     * @param hostedEntityId the host entity Id
     * @param realm          realm of the hosted IDP
     */
    @Override
    public void initialize(String hostedEntityId, String realm) {
        // Not relevant for Scripted IDP Adapter
    }

    @Override
    public boolean preSingleSignOn(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, String reqId)
            throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

        ScriptBindings scriptBindings = IdpBindings.preSingleSignOn()
                .withResponse(response)
                .withRequestId(reqId)
                .withRequest(request)
                .withAuthnRequest(authnRequest)
                .withHostedEntityId(hostedEntityId)
                .withRealm(realm)
                .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ADAPTER.name(),
                        script.getId(), script.getName()))
                .withScriptName(script.getName())
                .build();

        Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());
        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptVariables, "preSingleSignOn");
    }

    @Override
    public boolean preAuthentication(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

        ScriptBindings scriptBindings =
                IdpBindings.preAuthentication()
                        .withRelayState(relayState)
                        .withSession(session)
                        .withResponse(response)
                        .withRequestId(reqId)
                        .withRequest(request)
                        .withAuthnRequest(authnRequest)
                        .withHostedEntityId(hostedEntityId)
                        .withRealm(realm)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ADAPTER.name(),
                                script.getId(), script.getName()))
                        .withScriptName(script.getName())
                        .build();

        Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());
        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptVariables, "preAuthentication");
    }

    @Override
    public boolean preSendResponse(AuthnRequest authnRequest, String hostedEntityId, String realm,
            HttpServletRequest request, HttpServletResponse response, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);

        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

        ScriptBindings scriptBindings =
                IdpBindings.preSendResponse()
                        .withRelayState(relayState)
                        .withSession(session)
                        .withResponse(response)
                        .withRequestId(reqId)
                        .withRequest(request)
                        .withAuthnRequest(authnRequest)
                        .withHostedEntityId(hostedEntityId)
                        .withRealm(realm)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ADAPTER.name(),
                                script.getId(), script.getName()))
                        .withScriptName(script.getName())
                        .build();

        Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());
        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptVariables, "preSendResponse");
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostedEntityId, String realm,
            HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

        ScriptBindings scriptBindings =
                IdpBindings.preSignResponse()
                        .withRelayState(relayState)
                        .withSession(session)
                        .withSaml2Response(res)
                        .withRequest(request)
                        .withAuthnRequest(authnRequest)
                        .withHostedEntityId(hostedEntityId)
                        .withRealm(realm)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ADAPTER.name(),
                                script.getId(), script.getName()))
                        .withScriptName(script.getName())
                        .build();

        Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());
        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, scriptVariables, "preSignResponse");

    }

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

        ScriptBindings scriptBindings =
                IdpBindings.preSendFailureResponse()
                        .withRequest(request)
                        .withResponse(response)
                        .withFaultCode(faultCode)
                        .withFaultDetail(faultDetail)
                        .withHostedEntityId(hostedEntityId)
                        .withRealm(realm)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ADAPTER.name(),
                                script.getId(), script.getName()))
                        .withScriptName(script.getName())
                        .build();

        Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());
        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, scriptVariables, "preSendFailureResponse");
    }

    private void validateCommonAttributes(String hostedEntityId, String realm) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
    }

}
