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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * Immutable container for the result of processing a node.
 *
 * @supported.all.api
 * @since AM 5.5.0
 */
public final class Action {
    @Inject
    @Named("SystemSessionProperties")
    static List<String> blacklistedSessionProperties;

    /**
     * The output state of the node.
     */
    public final JsonValue sharedState;
    /**
     * Result of the node. May be null.
     **/
    public final String outcome;
    /**
     * Callbacks requested by the node when the outcome is null. May be null.
     */
    public final List<Callback> callbacks;
    /**
     * Properties that will be included in the user's session if/when it is created.
     */
    public final Map<String, String> sessionProperties;

    /**
     * Move on to the next node in the tree that is connected to the given outcome.
     * @param outcome the outcome.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder goTo(String outcome) {
        Reject.ifNull(outcome);
        return new ActionBuilder(outcome);
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     * @param callbacks a non-empty list of callbacks.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder send(List<Callback> callbacks) {
        Reject.ifNull(callbacks);
        Reject.ifTrue(callbacks.isEmpty(), "Callbacks must not be empty");
        return new ActionBuilder(callbacks);
    }

    /**
     * Send the given callbacks to the user for them to interact with.
     * @param callbacks a non-empty list of callbacks.
     * @return an action builder to provide additional details.
     */
    public static ActionBuilder send(Callback... callbacks) {
        return send(asList(callbacks));
    }

    private Action(JsonValue sharedState, String outcome, List<Callback> callbacks,
            Map<String, String> sessionProperties) {
        this.sharedState = sharedState;
        this.outcome = outcome;
        this.callbacks = Collections.unmodifiableList(callbacks);
        this.sessionProperties = Collections.unmodifiableMap(sessionProperties);
    }

    /**
     * Returns true if the action is a request for input.
     * @return true if the action need to request for input, false otherwise.
     */
    public boolean sendingCallbacks() {
        return !callbacks.isEmpty();
    }

    /**
     * Builder for the {@link Action}.
     */
    public static final class ActionBuilder {
        private JsonValue sharedState;
        private final String outcome;
        private final List<Callback> callbacks;
        private final Map<String, String> sessionProperties = new HashMap<>();

        private ActionBuilder(String outcome) {
            this.outcome = outcome;
            this.callbacks = Collections.emptyList();
        }

        private ActionBuilder(List<Callback> callbacks) {
            this.outcome = null;
            this.callbacks = callbacks;
        }

        /**
         * Replace the shared state.
         * @param sharedState the new state.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder replaceSharedState(JsonValue sharedState) {
            Reject.ifNull(sharedState);
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Add a new property.
         * @param key The key for the property.
         * @param value The value for the property.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder putSessionProperty(String key, String value) {
            Reject.ifNull(key, value);
            Reject.ifTrue(blacklistedSessionProperties != null && blacklistedSessionProperties.contains(key),
                    "Cannot set system session property " + key);
            sessionProperties.put(key, value);
            return this;
        }

        /**
         * Remove a previously set session property.
         * @param key The key for the property.
         * @return the same instance of the ActionBuilder.
         */
        public ActionBuilder removeSessionProperty(String key) {
            Reject.ifNull(key);
            Reject.ifTrue(blacklistedSessionProperties != null && blacklistedSessionProperties.contains(key),
                    "Cannot remove system session property " + key);
            sessionProperties.put(key, null);
            return this;
        }

        /**
         * Build the Action.
         * @return an Action.
         * @throws IllegalStateException if the builder is called before building mandatory fields.
         */
        public Action build() {
            return new Action(this.sharedState, this.outcome, this.callbacks, sessionProperties);
        }
    }
}
