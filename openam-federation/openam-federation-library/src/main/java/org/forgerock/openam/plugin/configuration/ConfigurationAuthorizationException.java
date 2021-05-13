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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.plugin.configuration;

import com.sun.identity.plugin.configuration.ConfigurationException;

/**
 * A sub-exception of {@link ConfigurationException} for {@code ConfigurationInstanceImpl}. This exception type
 * indicates that a Service Configuration action could not be performed due to insufficient user authorization.
 */
public class ConfigurationAuthorizationException extends ConfigurationException {

    /**
     * A constructor with a message.
     *
     * @param message The message.
     */
    public ConfigurationAuthorizationException(String message) {
        super(message);
    }

    /**
     * A constructor with a throwable.
     *
     * @param e The throwable.
     */
    public ConfigurationAuthorizationException(Throwable e) {
        super(e);
    }
}
