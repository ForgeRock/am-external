/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import static java.util.Collections.unmodifiableList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;

/**
 * A representation of the context of the current tree authentication process.
 *
 * @supported.all.api
 */
public final class TreeContext {

    /** The shared state that has accumulated so far by traversing the tree. */
    public final JsonValue sharedState;
    /** The HTTP request associated with the current authentication request. */
    public final ExternalRequestContext request;
    /** The state that is transient and that won't be stored across requests. */
    public final JsonValue transientState;

    private final List<? extends Callback> callbacks;

    /**
     * Construct a tree context for the current state.
     * @param sharedState The shared state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     */
    public TreeContext(JsonValue sharedState, ExternalRequestContext request, List<? extends Callback> callbacks) {
        this(sharedState, json(object()), request, callbacks);
    }

    /**
     * Construct a tree context for the current state.
     * @param sharedState The shared state.
     * @param transientState The transient state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authentication request.
     */
    public TreeContext(JsonValue sharedState, JsonValue transientState,
            ExternalRequestContext request, List<? extends Callback> callbacks) {
        this.sharedState = checkNotNull(sharedState);
        this.transientState = checkNotNull(transientState);
        this.request = checkNotNull(request);
        this.callbacks = unmodifiableList(checkNotNull(callbacks));
    }

    /**
     * Get the first callback of a particular type from the callbacks in the context.
     * @param callbackType The type of callback.
     * @param <T> The generic type of the callback.
     * @return An optional of the callback or empty if no callback of that type existed.
     */
    public <T extends Callback> Optional<T> getCallback(Class<T> callbackType) {
        return callbacks.stream()
                .filter(c -> callbackType.isAssignableFrom(c.getClass()))
                .map(callbackType::cast)
                .findFirst();
    }

    /**
     * Get the callbacks of a particular type from the callbacks in the context.
     * @param callbackType The type of callback.
     * @param <T> The generic type of the callback.
     * @return An list of callbacks or empty if no callback of that type existed.
     */
    public <T extends Callback> List<T> getCallbacks(Class<T> callbackType) {
        return callbacks.stream()
                .filter(c -> callbackType.isAssignableFrom(c.getClass()))
                .map(callbackType::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets all the callbacks sent in the request.
     *
     * <p>
     *     Use {@link #getCallback(Class)} in preference to this method.
     * </p>
     *
     * @return An unmodifiable list of callbacks.
     */
    public List<? extends Callback> getAllCallbacks() {
        return callbacks;
    }

    /**
     * Check if any callbacks have been submitted in this authenticate request.
     * @return {@code true} if there are callbacks available.
     */
    public boolean hasCallbacks() {
        return !callbacks.isEmpty();
    }
}
