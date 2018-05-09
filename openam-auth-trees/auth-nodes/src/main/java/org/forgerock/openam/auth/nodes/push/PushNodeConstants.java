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
 * Copyright 2018 ForgeRock AS.
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
