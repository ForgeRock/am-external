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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

/**
 * Constants used by the Journey Nodes.
 */
final class BackChannelNodeConstants {

    private BackChannelNodeConstants() {
        // Prevent instantiation
    }

    /**
     * The key used to store the transaction ID in the state.
     */
    static final String TRANSACTION_ID_STATE_KEY = "backchannel-transaction";
    /**
     * The key used to store the redirect URI in the state.
     */
    static final String REDIRECT_URI_STATE_KEY = "backchannel-redirectUri";
    /**
     * The key used to store the session properties in the state.
     */
    static final String JOURNEY_SESSION_PROPERTIES = "backchannel-sessionProperties";
}
