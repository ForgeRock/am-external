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
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.AUTHN_REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_CODE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.FAULT_DETAIL;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ADAPTER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGGER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQUEST;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REQ_ID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SAML2_RESPONSE;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.service.Saml2ScriptContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;

import com.google.inject.Inject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.shared.debug.Debug;

/**
 * Scripted implementation of the {@link com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter}
 */
public class ScriptedIdpAdapter implements SAML2IdentityProviderAdapter {

    private final ScriptEvaluator scriptEvaluator;
    private final ScriptStoreFactory scriptStoreFactory;
    private final ScriptEvaluationHelper scriptEvaluationHelper;
    private final ValidationHelper validationHelper;
    private final IdpAdapterScriptHelper idpAdapterScriptHelper;
    private final RealmLookup realmLookup;

    @Inject
    public ScriptedIdpAdapter(ScriptEvaluatorFactory scriptEvaluatorFactory,
            ScriptStoreFactory scriptStoreFactory,
            ScriptEvaluationHelper scriptEvaluationHelper,
            ValidationHelper validationHelper,
            IdpAdapterScriptHelper idpAdapterScriptHelper,
            RealmLookup realmLookup) {
        this.scriptEvaluator = scriptEvaluatorFactory.create(Saml2ScriptContext.SAML2_IDP_ADAPTER);
        this.scriptStoreFactory = scriptStoreFactory;
        this.scriptEvaluationHelper = scriptEvaluationHelper;
        this.validationHelper = validationHelper;
        this.idpAdapterScriptHelper = idpAdapterScriptHelper;
        this.realmLookup = realmLookup;
    }

    /**
     * This initialise method is empty as it is not called for Scripted IDP Adapters.
     * @param hostedEntityId the host entity Id
     * @param realm realm of the hosted IDP
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
        Bindings scriptVariables = createRequestBindings(hostedEntityId, realm, request, response, authnRequest, reqId);
        return evaluateScriptFunction(hostedEntityId, realm, scriptVariables, "preSingleSignOn");
    }

    @Override
    public boolean preAuthentication(String hostedEntityId, String realm, HttpServletRequest request,
                                     HttpServletResponse response, AuthnRequest authnRequest, Object session,
                                     String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        Bindings scriptVariables = createRequestBindings(hostedEntityId, realm, request, response, authnRequest, reqId);
        scriptVariables.put(SESSION, session);
        scriptVariables.put(RELAY_STATE, relayState);
        return evaluateScriptFunction(hostedEntityId, realm, scriptVariables, "preAuthentication");
    }

    @Override
    public boolean preSendResponse(AuthnRequest authnRequest, String hostedEntityId, String realm,
                                   HttpServletRequest request, HttpServletResponse response, Object session,
                                   String reqId, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);

        Bindings scriptVariables = createRequestBindings(hostedEntityId, realm, request, response, authnRequest, reqId);
        scriptVariables.put(SESSION, session);
        scriptVariables.put(RELAY_STATE, relayState);
        return evaluateScriptFunction(hostedEntityId, realm, scriptVariables, "preSendResponse");
    }

    @Override
    public void preSignResponse(AuthnRequest authnRequest, Response res, String hostedEntityId, String realm,
                                HttpServletRequest request, Object session, String relayState) throws SAML2Exception {
        validateCommonAttributes(hostedEntityId, realm);
        validationHelper.validateSession(session);
        Bindings scriptVariables = createPreResponseBindings(hostedEntityId, realm, request, authnRequest);
        scriptVariables.put(SESSION, session);
        scriptVariables.put(RELAY_STATE, relayState);
        scriptVariables.put(SAML2_RESPONSE, res);
        evaluateVoidScriptFunction(hostedEntityId, realm, scriptVariables, "preSignResponse");
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
        Bindings scriptVariables = createCommonBindings(hostedEntityId, realm);
        scriptVariables.put(REQUEST, wrapRequest(request));
        scriptVariables.put(RESPONSE, wrapResponse(response));
        scriptVariables.put(FAULT_CODE, faultCode);
        scriptVariables.put(FAULT_DETAIL, faultDetail);
        evaluateVoidScriptFunction(hostedEntityId, realm, scriptVariables, "preSendFailureResponse");
    }


    private void validateCommonAttributes(String hostedEntityId, String realm) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
    }

    private Bindings createCommonBindings(String hostedEntityId, String realm) {
        Bindings scriptVariables = new SimpleBindings();
        scriptVariables.put(HOSTED_ENTITYID, hostedEntityId);
        scriptVariables.put(REALM, realm);
        scriptVariables.put(IDP_ADAPTER_SCRIPT_HELPER, idpAdapterScriptHelper);
        return scriptVariables;
    }

    private Bindings createPreResponseBindings(String hostedEntityId, String realm, HttpServletRequest request,
                                               AuthnRequest authnRequest) {
        Bindings scriptVariables = createCommonBindings(hostedEntityId, realm);
        scriptVariables.put(REQUEST, wrapRequest(request));
        scriptVariables.put(AUTHN_REQUEST, authnRequest);
        return scriptVariables;
    }

    private Bindings createRequestBindings(String hostedEntityId, String realm, HttpServletRequest request,
                                           HttpServletResponse response, AuthnRequest authnRequest, String reqId) {
        Bindings scriptVariables = createPreResponseBindings(hostedEntityId, realm, request, authnRequest);
        scriptVariables.put(RESPONSE, wrapResponse(response));
        scriptVariables.put(REQ_ID, reqId);
        return scriptVariables;
    }

    private Script getScriptConfiguration(String realm, String hostedEntityID)
            throws ScriptException {
        String idpAttributeMapperScript = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(realm, hostedEntityID,
                SAML2Constants.IDP_ADAPTER_SCRIPT);
        try {
            return scriptStoreFactory.create(realmLookup.lookup(realm)).get(idpAttributeMapperScript);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        }
    }

    private void evaluateVoidScriptFunction(String hostedEntityId, String realm, Bindings scriptVariables,
                                            String functionName) throws SAML2Exception {
        try {
            Script script = getScriptConfiguration(realm, hostedEntityId);
            setLoggerBinding(scriptVariables, script);
            scriptEvaluationHelper.evaluateFunction(scriptEvaluator, script, scriptVariables, functionName);
        } catch (javax.script.ScriptException | ScriptException e) {
            throw new SAML2Exception(e);
        }
    }

    private boolean evaluateScriptFunction(String hostedEntityId, String realm, Bindings scriptVariables,
                                           String functionName) throws SAML2Exception {
        try {
            Script script = getScriptConfiguration(realm, hostedEntityId);
            setLoggerBinding(scriptVariables, script);
            return scriptEvaluationHelper.evaluateFunction(scriptEvaluator, script, scriptVariables,
                    Boolean.class, functionName).orElse(false);
        } catch (javax.script.ScriptException | ScriptException e) {
            throw new SAML2Exception(e);
        }
    }

    private void setLoggerBinding(Bindings scriptVariables, Script script) {
        scriptVariables.put(LOGGER, Debug.getInstance(String.format("scripts.%s", script.getName())));
    }

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request);
    }
    private HttpServletResponse wrapResponse(HttpServletResponse response) {
        return new HttpServletResponseWrapper(response);
    }
}
