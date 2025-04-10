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
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.scripted;

import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_SERVER_SIDE;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.DNMapper;

/**
 * An authentication module that allows users to authenticate via a scripting language
 */
public class Scripted extends AMLoginModule {
    private static final String ATTR_NAME_PREFIX = "iplanet-am-auth-scripted-";
    private static final String CLIENT_SCRIPT_ATTR_NAME = ATTR_NAME_PREFIX + "client-script";
    private static final String CLIENT_SCRIPT_ENABLED_ATTR_NAME = ATTR_NAME_PREFIX + "client-script-enabled";
    private static final String SERVER_SCRIPT_ATTRIBUTE_NAME = ATTR_NAME_PREFIX + "server-script";

    private final static int STATE_RUN_SCRIPT = 2;
    public static final String STATE_VARIABLE_NAME = "authState";
    public static final int SUCCESS_VALUE = -1;
    public static final int FAILURE_VALUE = -2;
    public static final String USERNAME_VARIABLE_NAME = "username";
    public static final String REALM_VARIABLE_NAME = "realm";
    public static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    public static final String IDENTITY_REPOSITORY = "idRepository";
    // Incoming from client side:
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME = "clientScriptOutputData";
    // Outgoing to server side:
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME = "clientScriptOutputData";
    public static final String REQUEST_DATA_VARIABLE_NAME = "requestData";
    public static final String SHARED_STATE = "sharedState";
    // Provides the ability for a script to define a failure URL to redirect to on authentication failure
    public static final String SHARED_STATE_FAILURE_URL_NAME = "gotoOnFailureUrl";

    private String userName;
    private String realm;
    private boolean clientSideScriptEnabled;
    private ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
    private ScriptStore scriptStore;
    public Map moduleConfiguration;

    /**
     * Debug logger instance used by scripts to log error/debug messages.
     */
    private static final Logger DEBUG = LoggerFactory.getLogger(Scripted.class);

    final HttpClientRequestFactory httpClientRequestFactory = InjectorHolder.getInstance(HttpClientRequestFactory.class);
    final ScriptHttpClientFactory httpClientFactory = InjectorHolder.getInstance(ScriptHttpClientFactory.class);
    private ChfHttpClient httpClient;
    private ScriptIdentityRepository identityRepository;
    protected Map<String, Object> sharedState;
    private Set<String> userSearchAttributes = Collections.emptySet();

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;

        userName = (String) sharedState.get(getUserKey());
        realm = DNMapper.orgNameToRealmName(getRequestOrg());
        moduleConfiguration = options;

        scriptStore = initialiseScriptingService();
        scriptEvaluator = getScriptEvaluator();
        clientSideScriptEnabled = getClientSideScriptEnabled();
        httpClient = getHttpClient();
        try {
            userSearchAttributes = getUserAliasList();
        } catch (final AuthLoginException ale) {
            DEBUG.warn("Scripted.init: unable to retrieve search attributes", ale);
        }
        identityRepository = getScriptIdentityRepository(userSearchAttributes);
    }

    private ScriptIdentityRepository getScriptIdentityRepository(Set<String> userSearchAttributes) {
        return new ScriptIdentityRepository(getIdentityStore(), userSearchAttributes);
    }

    private IdentityStore getIdentityStore() {
        return getIdentityStore(getRequestOrg());
    }

    private ScriptStore initialiseScriptingService() {
        ScriptStoreFactory scriptStoreFactory = InjectorHolder.getInstance(ScriptStoreFactory.class);
        try {
            return scriptStoreFactory.create(Realms.of(realm));
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {

        case ISAuthConstants.LOGIN_START:

            if (getClientSideScriptEnabled()) {
                substituteUIStrings();
                return STATE_RUN_SCRIPT;
            } else {
                // No client-side script, so immediately evaluate the server-side script with a null input
                return evaluateServerSideScript(null, STATE_RUN_SCRIPT);
            }

        case STATE_RUN_SCRIPT:
            String clientScriptOutputData = getClientScriptOutputData(callbacks);
            return evaluateServerSideScript(clientScriptOutputData, state);
        default:
            throw new AuthLoginException("Invalid state");
        }

    }

    private int evaluateServerSideScript(String clientScriptOutputData, int state) throws AuthLoginException {
        Script script = getServerSideScript();

        ServerSideAuthenticationScriptBindings scriptBindings = ServerSideAuthenticationScriptBindings.builder()
                .withRequestData(getScriptHttpRequestWrapper())
                .withClientScriptOutputData(clientScriptOutputData)
                .withState(state)
                .withSharedState(sharedState)
                .withUsername(userName)
                .withSuccessValue(SUCCESS_VALUE)
                .withFailureValue(FAILURE_VALUE)
                .withHttpClient(httpClient)
                .withIdentityRepository(identityRepository)
                .build();

        Bindings bindings;
        try {
            ScriptResult<Object> scriptResult = scriptEvaluator.evaluateScript(script, scriptBindings, Realms.of(realm));
            bindings = scriptResult.getBindings();
        } catch (ScriptException e) {
            DEBUG.debug("Error running server side scripts", e);
            throw new AuthLoginException("Error running script", e);
        } catch (RealmLookupException e) {
            DEBUG.debug("Realm not found when running script", e);
            throw new AuthLoginException("Error running script", e);
        }

        state = ((Number) bindings.get(STATE_VARIABLE_NAME)).intValue();
        userName = (String) bindings.get(USERNAME_VARIABLE_NAME);
        sharedState.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputData);

        if (state != SUCCESS_VALUE) {
            String scriptGotoOnFailureUrl = (String) sharedState.get(SHARED_STATE_FAILURE_URL_NAME);
            if (StringUtils.isNotEmpty(scriptGotoOnFailureUrl)) {
                sharedState.remove(SHARED_STATE_FAILURE_URL_NAME);
                setLoginFailureURL(scriptGotoOnFailureUrl);
            }
            throw new AuthLoginException("Authentication failed");
        }

        return state;
    }

    private String getClientScriptOutputData(Callback[] callbacks) {
        String clientScriptOutputData = ((HiddenValueCallback) callbacks[0]).getValue();
        if (clientScriptOutputData == null) { // To cope with the classic UI
            clientScriptOutputData = getScriptHttpRequestWrapper().
                    getParameter(CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME);
        }
        return clientScriptOutputData;
    }

    private Script getServerSideScript() throws AuthLoginException {
        String scriptId = getConfigValue(SERVER_SCRIPT_ATTRIBUTE_NAME);
        try {
            if (ChoiceValues.EMPTY_SCRIPT_SELECTION.equals(scriptId)) {
                return Script.EMPTY_SCRIPT;
            }
            return scriptStore.get(scriptId);
        } catch (org.forgerock.openam.scripting.domain.ScriptException e) {
            DEBUG.error("Error retrieving server side script", e);
            throw new AuthLoginException("Error retrieving script", e);
        }
    }

    private ScriptEvaluator getScriptEvaluator() {
        return InjectorHolder.getInstance(ScriptEvaluatorFactory.class).create(AUTHENTICATION_SERVER_SIDE);
    }

    private ChfHttpClient getHttpClient() {
        ScriptingLanguage scriptType = getScriptType();

        if (scriptType == null) {
            return null;
        }

        return httpClientFactory.getScriptHttpClient(scriptType);
    }

    private HttpClientRequest getHttpRequest() {
        return httpClientRequestFactory.createRequest();
    }

    private String getClientSideScript() {
        String script = "";
        if (!clientSideScriptEnabled) {
            return script;
        }

        String clientSideScriptId = getConfigValue(CLIENT_SCRIPT_ATTR_NAME);
        if (ChoiceValues.EMPTY_SCRIPT_SELECTION.equals(clientSideScriptId)) {
            return script;
        }

        try {
            script = scriptStore.get(clientSideScriptId).getScript();
        } catch (org.forgerock.openam.scripting.domain.ScriptException e) {
            DEBUG.error("Error retrieving client side script", e);
        }
        return script;
    }

    private String getConfigValue(String attributeName) {
        return CollectionHelper.getMapAttr(moduleConfiguration, attributeName);
    }

    private ScriptHttpRequestWrapper getScriptHttpRequestWrapper() {
        return new ScriptHttpRequestWrapper(getHttpServletRequest());
    }

    private void substituteUIStrings() throws AuthLoginException {
        replaceCallback(STATE_RUN_SCRIPT, 1, createClientSideScriptAndSelfSubmitCallback());
    }

    private Callback createClientSideScriptAndSelfSubmitCallback() {
        String clientSideScriptExecutorFunction = ScriptedClientUtilityFunctions.
                createClientSideScriptExecutorFunction(getClientSideScript(), CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME);
        ScriptTextOutputCallback scriptAndSelfSubmitCallback =
                new ScriptTextOutputCallback(clientSideScriptExecutorFunction);

        return scriptAndSelfSubmitCallback;
    }

    private ScriptingLanguage getScriptType() {
        try {
            return getServerSideScript().getLanguage();
        } catch (AuthLoginException e) {
            DEBUG.error("Error retrieving server side scripting language", e);
        }
        return null;
    }

    private boolean getClientSideScriptEnabled() {
        String clientSideScriptEnabled = getConfigValue(CLIENT_SCRIPT_ENABLED_ATTR_NAME);
        return Boolean.parseBoolean(clientSideScriptEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        if (userName == null) {
            DEBUG.debug("Warning: username is null");
        }

        return new ScriptedPrinciple(userName);
    }
}
