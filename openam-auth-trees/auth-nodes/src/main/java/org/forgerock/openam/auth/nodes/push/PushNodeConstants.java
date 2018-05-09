/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

/**
 * Shared constants use by the push authentication nodes.
 */
final class PushNodeConstants {

    private PushNodeConstants() {
        // do nothing
    }

    /**
     * MESSAGE CODE KEYS.
     */

    /** The key for the mechanism id. */
    static final String MECHANISM_ID_KEY = "u";
    /** The key for the challenge. */
    static final String CHALLENGE_KEY = "c";
    /** The key for the loadbalancer. */
    static final String LOADBALANCER_KEY = "l";
    /** The key for the time to live. */
    static final String TIME_TO_LIVE_KEY = "t";

    /**
     * SHARED STATE KEYS.
     */
    static final String MESSAGE_ID_KEY = "pushMessageId";

    /**
     * The name of the JSON field which holds the push response's content inside the authentication shared state.
     */
    public static final String PUSH_CONTENT_KEY = "pushContent";
}
