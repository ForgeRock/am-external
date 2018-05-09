/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for wrapping a Jaspi ServerAuthModule. Provides wrappers over the modules functionality, and hides
 * the underlying code from external users.
 * @param <T> Implementation of the ServerAuthModule interface.
 */

public abstract class JaspiAuthModuleWrapper<T extends ServerAuthModule> {

    private final T serverAuthModule;

    /**
     * Create the wrapper by passing it the module it wraps. Designed to be called by a subclass which provides the
     * instance.
     * @param serverAuthModule The module to wrap.
     */
    protected JaspiAuthModuleWrapper(T serverAuthModule) {
        this.serverAuthModule = serverAuthModule;
    }

    public void initialize(CallbackHandler callbackHandler, Map<String, Object> config)
            throws AuthException {
        serverAuthModule.initialize(createRequestMessagePolicy(), null, callbackHandler, config);
    }

    /**
     * Creates a MessageInfo instance containing the given HttpServletRequest and HttpServletResponse.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @return A MessageInfo instance.
     */
    public MessageInfo prepareMessageInfo(final HttpServletRequest request, final HttpServletResponse response) {
        final HashMap<Object, Object> properties = new HashMap<>();
        return new MessageInfo() {

            @Override
            public Object getRequestMessage() {
                return request;
            }

            @Override
            public Object getResponseMessage() {
                return response;
            }

            @Override
            public void setRequestMessage(Object ignored) {
                //Not able to set request
            }

            @Override
            public void setResponseMessage(Object ignored) {
                //Not able to set response
            }

            @Override
            public Map getMap() {
                return properties;
            }
        };
    }

    /**
     * Calls through to the validateRequest of the underlying module.
     * @param messageInfo Object capturing information about the ongoing request.
     * @param clientSubject Subject used to store principals and credentials used in authentication.
     * @return An object indicating the status of the operation. See ServerAuth for more information.
     * @throws AuthException If something goes wrong.
     */
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject) throws AuthException {
        return serverAuthModule.validateRequest(messageInfo, clientSubject, null);
    }

    /**
     * Calls through to the secureResponse method of the underlying module.
     * @param messageInfo Object capturing information about the ongoing request.
     * @return An object indicating the status of the operation. See ServerAuth for more information.
     * @throws AuthException If something goes wrong.
     */
    public AuthStatus secureResponse(MessageInfo messageInfo) throws AuthException {
        return serverAuthModule.secureResponse(messageInfo, null);
    }

    /**
     * Creates a MessagePolicy instance.
     *
     * @return A MessagePolicy instance.
     */
    private MessagePolicy createRequestMessagePolicy() {
        MessagePolicy.Target[] targets = new MessagePolicy.Target[]{};
        MessagePolicy.ProtectionPolicy protectionPolicy = new MessagePolicy.ProtectionPolicy() {
            @Override
            public String getID() {
                return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
            }
        };
        MessagePolicy.TargetPolicy targetPolicy = new MessagePolicy.TargetPolicy(targets, protectionPolicy);
        MessagePolicy.TargetPolicy[] targetPolicies = new MessagePolicy.TargetPolicy[]{targetPolicy};
        return new MessagePolicy(targetPolicies, true);
    }

    /**
     * Gets the underlying JASPI ServerAuthModule instance.
     *
     * @return The JASPI ServerAuthModule instance.
     */
    protected T getServerAuthModule() {
        return serverAuthModule;
    }

}
