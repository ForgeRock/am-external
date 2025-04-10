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
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.saml2.plugins.IDPAdapter;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

import com.google.inject.Inject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlBindings.IdpBindings;
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

    private final ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
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

        SamlIdpPreSingleSignOnBindings scriptBindings = IdpBindings.preSingleSignOn()
                .withResponse(response)
                .withRequestId(reqId)
                .withRequest(request)
                .withAuthnRequest(authnRequest)
                .withHostedEntityId(hostedEntityId)
                .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                .build();

        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptBindings, "preSingleSignOn");
    }

    @Override
    public boolean preAuthentication(String hostedEntityId, String realm, HttpServletRequest request,
            HttpServletResponse response, AuthnRequest authnRequest, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

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
                        .build();

        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptBindings, "preAuthentication");
    }

    @Override
    public boolean preSendResponse(AuthnRequest authnRequest, String hostedEntityId, String realm,
            HttpServletRequest request, HttpServletResponse response, Object session,
            String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);

        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

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
                        .build();

        return executor.evaluateScriptFunction(scriptEvaluator, script, realm, scriptBindings, "preSendResponse");
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostedEntityId, String realm,
            HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, IDP_ROLE, IDP_ADAPTER_SCRIPT);

        SamlIdpPreSignResponseBindings scriptBindings =
                IdpBindings.preSignResponse()
                        .withRelayState(relayState)
                        .withSession(session)
                        .withSaml2Response(res)
                        .withRequest(request)
                        .withAuthnRequest(authnRequest)
                        .withHostedEntityId(hostedEntityId)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .build();

        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, scriptBindings, "preSignResponse");

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

        SamlIdpPreSendFailureResponseBindings scriptBindings =
                IdpBindings.preSendFailureResponse()
                        .withRequest(request)
                        .withResponse(response)
                        .withFaultCode(faultCode)
                        .withFaultDetail(faultDetail)
                        .withHostedEntityId(hostedEntityId)
                        .withIdpAdapterScriptHelper(idpAdapterScriptHelper)
                        .build();

        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, scriptBindings, "preSendFailureResponse");
    }

    private void validateCommonAttributes(String hostedEntityId, String realm) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
    }

}
