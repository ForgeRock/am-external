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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

/**
 * Shared constants use by the push authentication nodes.
 */
public final class PushNodeConstants {

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
    /** The key for the time interval. */
    static final String TIME_INTERVAL_KEY = "i";
    /** The key for the custom payload. */
    static final String CUSTOM_PAYLOAD_KEY = "p";
    /** The key for the notification message. */
    static final String NOTIFICATION_MESSAGE_KEY = "m";
    /** The key for the push notification type. */
    static final String PUSH_TYPE_KEY = "k";
    /** The key for the challenge numbers type. */
    static final String NUMBERS_CHALLENGE_KEY = "n";
    /** The key for the context info. */
    static final String CONTEXT_INFO_KEY = "x";
    /** The key for the user id. */
    static final String USER_ID_KEY = "d";
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
    /** The key for the PUSH resource id query component of the QR code. */
    static final String PUSH_RESOURCE_ID_KEY = "pid";
    /** The key for URI schema. */
    static final String PUSH_URI_SCHEME_QR_CODE_KEY = "pushauth";
    /** The key for host. */
    static final String PUSH_URI_HOST_QR_CODE_KEY = "push";


    /**
     * SHARED STATE KEYS.
     */

    /** The key for the messageId. */
    public static final String MESSAGE_ID_KEY = "pushMessageId";
    /** The key for the push challenge. */
    public static final String PUSH_CHALLENGE_KEY = "pushChallengeKey";
    /** The key for the push device profile. */
    public static final String PUSH_DEVICE_PROFILE_KEY = "pushDeviceProfile";
    /** The key for the registration timeout. */
    public static final String PUSH_REGISTRATION_TIMEOUT = "pushRegistrationTimeout";
    /** The key for the number challenge. */
    public static final String PUSH_NUMBER_CHALLENGE_KEY = "pushNumberChallengeKey";
    /** The key for the message expiration. */
    public static final String PUSH_MESSAGE_EXPIRATION = "pushMessageExpiration";
    /** The key for the mechanismUid. */
    public static final String PUSH_MECHANISM_UID_KEY = "pushMechanismUid";

    /**
     * The name of the JSON field which holds the push response's content inside the authentication shared state.
     */
    static final String PUSH_CONTENT_KEY = "pushContent";
}
