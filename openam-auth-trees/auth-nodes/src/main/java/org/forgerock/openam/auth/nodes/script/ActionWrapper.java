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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static java.util.Arrays.asList;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.SuspendedTextOutputCallback;
import org.forgerock.openam.auth.node.api.SuspensionHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import com.sun.identity.idm.IdType;

/**
 * Acts as a wrapper of the Action.Builder class for script bindings.
 */
public final class ActionWrapper {

    private String outcome;
    private List<? extends Callback> callbacks = new ArrayList<>();
    private final Map<String, String> addSessionProperties = new HashMap<>();
    private final List<String> removeSessionProperties = new ArrayList<>();
    private Duration maxSessionTime;
    private Duration maxIdleTime;
    private String errorMessage;
    private String lockoutMessage;
    private String username;
    private IdType identityType;
    private String stage;
    private String header;
    private String description;
    private SuspensionHandler suspensionHandler;
    private Duration suspendDuration;

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
     * Set the maximum session time for the user, in minutes.
     *
     * @param maxSessionTime the maximum session time (minutes).
     * @return this action builder.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withMaxSessionTime(int maxSessionTime) {
        Reject.ifTrue(maxSessionTime < 1, "maximumSessionTime must be greater than 0");
        this.maxSessionTime = Duration.ofMinutes(maxSessionTime);
        return this;
    }

    /**
     * Set the maximum idle time for the user, in minutes.
     *
     * @param maxIdleTime the maximum idle time (minutes).
     * @return this action builder.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withMaxIdleTime(int maxIdleTime) {
        Reject.ifTrue(maxIdleTime < 1, "maximumIdleTime must be greater than 0");
        this.maxIdleTime = Duration.ofMinutes(maxIdleTime);
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
     * Sets the stage of the action.
     *
     * @param stage The stage to set.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withStage(String stage) {
        this.stage = stage;
        return this;
    }

    /**
     * Sets the header of the action.
     *
     * @param header The header of the action.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withHeader(String header) {
        this.header = header;
        return this;
    }

    /**
     * Sets the description of the action.
     *
     * @param description The description of the action.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Suspend the current authentication session and send the user the given callback.
     *
     * @param callbackTextFormat the text to display to the user.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper suspend(String callbackTextFormat) {
        return suspend(callbackTextFormat, (resumeURI) -> { });
    }

    /**
     * Suspend the current authentication session and send the user the given callback.
     *
     * @param callbackTextFormat the text to display to the user.
     * @param additionalLogic    additional logic to execute before suspending the session.
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper suspend(String callbackTextFormat, SuspensionLogic additionalLogic) {
        this.suspensionHandler = (resumeURI) -> {
            try {
                additionalLogic.execute(resumeURI);
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute additional logic", e);
            }
            return SuspendedTextOutputCallback.info(MessageFormat.format(callbackTextFormat, resumeURI));
        };
        return this;
    }

    /**
     * Suspend the current authentication session and send the user the given callback.
     *
     * @param callbackTextFormat the text to display to the user.
     * @param additionalLogic    additional logic to execute before suspending the session.
     * @param maximumSuspendDuration  the maximum duration to suspend the session for (minutes).
     * @return the same instance of the ActionWrapper.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public ActionWrapper suspend(String callbackTextFormat, SuspensionLogic additionalLogic,
            int maximumSuspendDuration) {
        Reject.ifTrue(maximumSuspendDuration < 1, "maximumSuspendDuration must be greater than 0");
        this.suspendDuration = Duration.ofMinutes(maximumSuspendDuration);
        return suspend(callbackTextFormat, additionalLogic);
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
        if (suspensionHandler != null) {
            return suspendDuration != null
                    ? Action.suspend(suspensionHandler, suspendDuration).build()
                    : Action.suspend(suspensionHandler).build();
        }

        Action.ActionBuilder actionBuilder = callbacks.isEmpty() ? Action.goTo(this.outcome) : Action.send(callbacks);
        addSessionProperties.forEach(actionBuilder::putSessionProperty);
        removeSessionProperties.forEach(actionBuilder::removeSessionProperty);
        actionBuilder.withMaxSessionTime(maxSessionTime);
        actionBuilder.withMaxIdleTime(maxIdleTime);
        actionBuilder.withErrorMessage(errorMessage);
        actionBuilder.withLockoutMessage(lockoutMessage);
        actionBuilder.withStage(stage);
        actionBuilder.withDescription(description);
        actionBuilder.withHeader(header);
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
        return this.callbacks.isEmpty() && StringUtils.isBlank(this.outcome) && this.suspensionHandler == null;
    }

    /**
     * Represents a callable function which can reference the resume URI at the point of suspension.
     */
    @FunctionalInterface
    public interface SuspensionLogic {
        /**
         * Execute additional logic before suspending the session.
         *
         * @param resumeUri the URI that will be used to resume authentication.
         */
        void execute(URI resumeUri);
    }
}
