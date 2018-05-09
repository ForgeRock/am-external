/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.wait;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.utils.Time;

/**
 * Contains common code used for creating a polling wait callback based node. Includes support for rendering, as well
 * as spam detection.
 */
public class WaitingHelper {

    private final String endTimeKey;
    private final String spamCountKey;
    private final int secondsToWait;

    /**
     * Constructor.
     * @param uuid A UUID which will remain consistent for future instantiators of this helper.
     * @param secondsToWait The number of seconds to create the callback for.
     */
    public WaitingHelper(UUID uuid, int secondsToWait) {
        this.secondsToWait = secondsToWait;
        this.endTimeKey = uuid.toString() + ".end_time";
        this.spamCountKey = uuid.toString() + ".spam_count";
    }

    /**
     * Produces the next state based on the values stored in this helper.
     * @param baseState The original state.
     * @return The state with appropriate values added.
     */
    public JsonValue getNextState(JsonValue baseState) {
        JsonValue state = baseState.copy();
        state.put(endTimeKey, getEndTime(baseState).toEpochMilli());
        state.put(spamCountKey, getSpamCount(state) + 1);
        return state;
    }

    /**
     * Checks whether the user has spammed this helper too much.
     * @param state The state of the tree.
     * @param maxCount The maximum intermediate requests allowed.
     * @return True if the user has spammed too much, false otherwise.
     */
    public boolean spamCountExceeds(JsonValue state, int maxCount) {
        return getSpamCount(state) > maxCount;
    }

    /**
     * Checks whether the user has waited long enough.
     * @param state The state of the tree.
     * @return True if the user has completed the wait, false otherwise.
     */
    public boolean waitTimeCompleted(JsonValue state) {
        return now().isAfter(getEndTime(state));
    }

    /**
     * Clears the provided state of keys used by this helper. Allows the provided UUID to be used more than once.
     * @param baseState The state of the tree.
     * @return The new state.
     */
    public JsonValue clearState(JsonValue baseState) {
        JsonValue newState = baseState.copy();
        newState.remove(endTimeKey);
        newState.remove(spamCountKey);
        return newState;
    }

    /**
     * Creates the callbacks required by this node.
     * @param message The message to display to the user. Assumed to be correctly localised.
     * @return The list of created callbacks.
     */
    public List<Callback> createCallbacks(String message) {
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(pollingWaitCallback(message));
        return callbacks;
    }

    private Instant now() {
        return Time.getClock().instant();
    }

    private Instant getEndTime(JsonValue sharedState) {
        if (!sharedState.isDefined(endTimeKey)) {
            return now().plusSeconds(secondsToWait);
        }
        return Instant.ofEpochMilli(sharedState.get(endTimeKey).asLong());
    }

    private int getSpamCount(JsonValue sharedState) {
        if (!sharedState.isDefined(spamCountKey)) {
            return -1;
        }
        return sharedState.get(spamCountKey).asInteger();
    }

    private Callback pollingWaitCallback(String message) {
        return new PollingWaitCallback.PollingWaitCallbackBuilder()
                .withWaitTime(String.valueOf(secondsToWait * 1000)).withMessage(message).build();
    }
}
