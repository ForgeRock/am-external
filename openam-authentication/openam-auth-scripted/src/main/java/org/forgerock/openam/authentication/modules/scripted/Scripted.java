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
 * Copyright 2014-2020 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted;

import static org.forgerock.openam.scripting.ScriptConstants.EMPTY_SCRIPT_SELECTION;
import static org.forgerock.openam.scripting.ScriptContext.AUTHENTICATION_SERVER_SIDE;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.factories.ScriptHttpClientFactory;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
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
    private static final String SUCCESS_ATTR_NAME = "SUCCESS";
    public static final int SUCCESS_VALUE = -1;
    private static final String FAILED_ATTR_NAME = "FAILED";
    public static final int FAILURE_VALUE = -2;
    public static final String USERNAME_VARIABLE_NAME = "username";
    public static final String REALM_VARIABLE_NAME = "realm";
    public static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    public static final String LOGGER_VARIABLE_NAME = "logger";
    public static final String IDENTITY_REPOSITORY = "idRepository";
    // Incoming from client side:
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME = "clientScriptOutputData";
    // Outgoing to server side:
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME = "clientScriptOutputData";
    public static final String REQUEST_DATA_VARIABLE_NAME = "requestData";
    public static final String SHARED_STATE = "sharedState";

    private String userName;
    private String realm;
    private boolean clientSideScriptEnabled;
    private ScriptEvaluator scriptEvaluator;
    private ScriptingService scriptingService;
    public Map moduleConfiguration;

    /** Debug logger instance used by scripts to log error/debug messages. */
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

        scriptingService = initialiseScriptingService();
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
        return new ScriptIdentityRepository(getAmIdentityRepository(), userSearchAttributes);
    }

    private AMIdentityRepository getAmIdentityRepository() {
        return getAMIdentityRepository(getRequestOrg());
    }

    private ScriptingService initialiseScriptingService() {
        ScriptingServiceFactory scriptingServiceFactory =
                InjectorHolder.getInstance(Key.get(new TypeLiteral<ScriptingServiceFactory>() {}));
        return scriptingServiceFactory.create(getRequestOrg());
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
        Pair<String, ScriptObject> script = getServerSideScript();
        Bindings scriptVariables = new SimpleBindings();
        scriptVariables.put(REQUEST_DATA_VARIABLE_NAME, getScriptHttpRequestWrapper());
        scriptVariables.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputData);
        scriptVariables.put(LOGGER_VARIABLE_NAME,
                Debug.getInstance("scripts." + AUTHENTICATION_SERVER_SIDE.name() + "." + script.getFirst()));
        scriptVariables.put(STATE_VARIABLE_NAME, state);
        scriptVariables.put(SHARED_STATE, sharedState);
        scriptVariables.put(USERNAME_VARIABLE_NAME, userName);
        scriptVariables.put(REALM_VARIABLE_NAME, realm);
        scriptVariables.put(SUCCESS_ATTR_NAME, SUCCESS_VALUE);
        scriptVariables.put(FAILED_ATTR_NAME, FAILURE_VALUE);
        scriptVariables.put(HTTP_CLIENT_VARIABLE_NAME, httpClient);
        scriptVariables.put(IDENTITY_REPOSITORY, identityRepository);

        try {
            scriptEvaluator.evaluateScript(script.getSecond(), scriptVariables);
        } catch (ScriptException e) {
            DEBUG.debug("Error running server side scripts", e);
            throw new AuthLoginException("Error running script", e);
        }

        state = ((Number) scriptVariables.get(STATE_VARIABLE_NAME)).intValue();
        userName = (String) scriptVariables.get(USERNAME_VARIABLE_NAME);
        sharedState.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputData);

        if (state != SUCCESS_VALUE) {
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

    private Pair<String, ScriptObject> getServerSideScript() throws AuthLoginException {
        String scriptId = getConfigValue(SERVER_SCRIPT_ATTRIBUTE_NAME);
        try {
            if (EMPTY_SCRIPT_SELECTION.equals(scriptId)) {
                return Pair.of(null, new ScriptObject("DefaultScript", "", SupportedScriptingLanguage.JAVASCRIPT));
            }
            ScriptConfiguration config = scriptingService.get(scriptId);
            return Pair.of(scriptId, new ScriptObject(config.getName(), config.getScript(), config.getLanguage()));
        } catch (org.forgerock.openam.scripting.ScriptException e) {
            DEBUG.error("Error retrieving server side script", e);
            throw new AuthLoginException("Error retrieving script", e);
        }
    }

    private ScriptEvaluator getScriptEvaluator() {
        return InjectorHolder.getInstance(
                Key.get(ScriptEvaluator.class, Names.named(AUTHENTICATION_SERVER_SIDE.name())));
    }

    private ChfHttpClient getHttpClient() {
        SupportedScriptingLanguage scriptType = getScriptType();

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
        if (EMPTY_SCRIPT_SELECTION.equals(clientSideScriptId)) {
            return script;
        }

        try {
            script = scriptingService.get(clientSideScriptId).getScript();
        } catch (org.forgerock.openam.scripting.ScriptException e) {
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

    private SupportedScriptingLanguage getScriptType() {
        try {
            return (SupportedScriptingLanguage)getServerSideScript().getSecond().getLanguage();
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
