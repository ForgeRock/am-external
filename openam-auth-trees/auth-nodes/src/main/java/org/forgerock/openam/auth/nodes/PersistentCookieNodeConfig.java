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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.session.SessionConstants.PERSISTENT_COOKIE_SESSION_PROPERTY;

/**
 * Implementations of this interface are responsible for supplying functionality related to
 * persistent cookie node config.
 */
public interface PersistentCookieNodeConfig {

    /**
     * The name of the persistent cookie.
     *
     * @return the name of the persistent cookie.
     */
    String getPersistentCookieName();

    /**
     * Convenience method used to generate the name used for the persistent cookie session property.
     * This should be used to generate a unique property name for each unique persistent cookie. The name must be
     * unique for each cookie otherwise only one cookie name will be stored in the session properties (and therefore
     * only the most recently stored cookie name will be cleared on logout - see OPENAM-19709).
     *
     * @param persistentCookieName the name of the persistent cookie that the property relates to.
     * @return the generated property name used to store/access the persistent cookie name property.
     */
    default String generateSessionPropertyName(String persistentCookieName) {
        return PERSISTENT_COOKIE_SESSION_PROPERTY + "_" + persistentCookieName;
    }
}
