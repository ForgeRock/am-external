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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static java.util.Arrays.asList;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import com.sun.identity.idm.IdType;

/**
 * Acts as a wrapper of the Action.Builder class for script bindings.
 */
public final class ActionWrapper {

    private String outcome;
    private List<? extends Callback> callbacks = new ArrayList<>();
    private Map<String, String> addSessionProperties = new HashMap<>();
    private List<String> removeSessionProperties = new ArrayList<>();
    private String errorMessage;
    private String lockoutMessage;
    private String username;
    private IdType identityType;

    /**
     * Move on to the next node in the tree that is connected to the given outcome.
     *
     * @param outcome the outcome.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper goTo(String outcome) {
        Reject.ifNull(outcome);
        this.outcome = outcome;
        return this;
    }

    /**
     * Add a new session property.
     *
     * @param key   the key of the session properties to be added to the session.
     * @param value the value of the session properties to be added to the session.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper putSessionProperty(String key, String value) {
        addSessionProperties.put(key, value);
        return this;
    }

    /**
     * Remove a previously set session property.
     *
     * @param key The key for the property.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper removeSessionProperty(String key) {
        removeSessionProperties.add(key);
        return this;
    }

    /**
     * Sets the error message to present to the caller when the FAILURE node is reached.
     *
     * <p>It is up to the caller to apply localisation.</p>
     *
     * @param errorMessage The error message.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Sets the lockout message to present to the caller when the user is locked out.
     *
     * <p>It is up to the caller to apply localisation.</p>
     *
     * @param lockoutMessage The lockout message.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withLockoutMessage(String lockoutMessage) {
        this.lockoutMessage = lockoutMessage;
        return this;
    }

    /**
     * Sets the username for the identified user identity.
     *
     * @param username The user username
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withIdentifiedUser(String username) {
        this.username = username;
        this.identityType = IdType.USER;
        return this;
    }

    /**
     * Sets the username for the identified agent identity.
     *
     * @param agentName The agent username
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withIdentifiedAgent(String agentName) {
        this.username = agentName;
        this.identityType = IdType.AGENT;
        return this;
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     *
     * @param callbacks a non-empty list of callbacks.
     */
    private void setCallbacks(List<? extends Callback> callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     *
     * @param callbacks a non-empty list of callbacks.
     */
    public void setCallbacks(Callback... callbacks) {
        setCallbacks(asList(callbacks));
    }

    /**
     * Build an Action object from the wrapped Action Builder.
     *
     * @return Action object.
     */
    public Action buildAction() {
        Action.ActionBuilder actionBuilder = callbacks.isEmpty() ? Action.goTo(this.outcome) : Action.send(callbacks);
        addSessionProperties.forEach(actionBuilder::putSessionProperty);
        removeSessionProperties.forEach(actionBuilder::removeSessionProperty);
        actionBuilder.withErrorMessage(errorMessage);
        actionBuilder.withLockoutMessage(lockoutMessage);
        if (isNotBlank(username) && identityType != null) {
            actionBuilder.withIdentifiedIdentity(username, identityType);
        }
        return actionBuilder.build();
    }

    /**
     * False if the ActionWrapper state has not changed during script evaluation.
     *
     * @return True if callback or action required.
     */
    public boolean isEmpty() {
        return this.callbacks.isEmpty() && StringUtils.isBlank(this.outcome);
    }
}
