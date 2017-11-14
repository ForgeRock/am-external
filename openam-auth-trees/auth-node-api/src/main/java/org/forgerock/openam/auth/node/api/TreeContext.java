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

import static java.util.Collections.unmodifiableList;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.List;
import java.util.Optional;

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

    private final List<? extends Callback> callbacks;

    /**
     * Construct a tree context for the current state.
     * @param sharedState The shared state.
     * @param request The request associated with the current authentication request.
     * @param callbacks The callbacks received in the current authenticate request.
     */
    public TreeContext(JsonValue sharedState, ExternalRequestContext request, List<? extends Callback> callbacks) {
        this.sharedState = checkNotNull(sharedState);
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
