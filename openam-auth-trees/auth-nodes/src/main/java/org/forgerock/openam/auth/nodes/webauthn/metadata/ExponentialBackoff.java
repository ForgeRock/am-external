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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import java.time.Duration;
import java.time.Instant;

import org.forgerock.openam.utils.Time;

/**
 * Utility class for handling exponential backoff.
 */
public class ExponentialBackoff {
    private final Duration base;
    private final Duration max;
    private Duration current;
    private Instant unreadyUntil;

    /**
     * Create a new instance of the ExponentialBackoff class.
     *
     * @param base The base duration to start with.
     * @param max The maximum duration to back off to.
     */
    public ExponentialBackoff(Duration base, Duration max) {
        this.base = base;
        this.max = max;
        reset();
    }

    /**
     * Check if the backoff is ready.
     *
     * @return true if the backoff is ready, false otherwise
     */
    public boolean isReady() {
        if (unreadyUntil == null) {
            return true;
        }
        if (Time.instant().isAfter(unreadyUntil)) {
            unreadyUntil = null;
            return true;
        }
        return false;
    }

    /**
     * Notify that an error occurred and back off.
     */
    public void error() {
        unreadyUntil = Time.instant().plus(current);
        if (current != max) { // check for same instance
            current = current.multipliedBy(2);
            if (current.compareTo(max) > 0) {
                current = max;
            }
        }
    }

    /**
     * Reset the backoff when the operation succeeded.
     */
    public void reset() {
        current = base;
        unreadyUntil = null;
    }
}
