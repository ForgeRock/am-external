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
 * Copyright 2018-2020 ForgeRock AS.
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
     * QR CODE KEYS.
     */

    /** The key for the Message Id query component of the QR code. */
    static final String MESSAGE_ID_QR_CODE_KEY = "m";
    /** The key for the shared secret query component of the QR code. */
    static final String SHARED_SECRET_QR_CODE_KEY = "s";
    /** The key for the registration url query component of the QR code. */
    static final String REG_QR_CODE_KEY = "r";
    /** The key for the authentication url query component of the QR code. */
    static final String AUTH_QR_CODE_KEY = "a";
    /** The key for the loadbalancer information component of the QR code. */
    static final String LOADBALANCER_DATA_QR_CODE_KEY = "l";
    /** The key for the JWS challenge for registration of the QR code. */
    static final String CHALLENGE_QR_CODE_KEY = "c";
    /** The key for URI schema. */
    static final String PUSH_URI_SCHEME_QR_CODE_KEY = "pushauth";
    /** The key for host. */
    static final String PUSH_URI_HOST_QR_CODE_KEY = "push";


    /**
     * SHARED STATE KEYS.
     */
    static final String MESSAGE_ID_KEY = "pushMessageId";
    static final String PUSH_CHALLENGE_KEY = "pushChallengeKey";
    static final String PUSH_DEVICE_PROFILE_KEY = "pushDeviceProfile";
    static final String PUSH_REGISTRATION_TIMEOUT = "pushRegistrationTimeout";

    /**
     * The name of the JSON field which holds the push response's content inside the authentication shared state.
     */
    static final String PUSH_CONTENT_KEY = "pushContent";
}