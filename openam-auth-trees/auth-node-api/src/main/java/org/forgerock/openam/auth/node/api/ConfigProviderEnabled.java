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
package org.forgerock.openam.auth.node.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.forgerock.openam.annotations.Supported;

/**
 * Indicates that an {@link OutcomeProvider} has been enabled to be used with the configuration provider node.
 *
 * <p>
 *     Note that this annotation is not required for any outcome providers which extend {@link StaticOutcomeProvider} or
 * {@link BoundedOutcomeProvider} as these are automatically enabled for use with the configuration provider node.
 *
 * <p>Outcome providers which rely on node configuration to generate outcomes should annotate every configuration
 * attribute that they rely on with {@link ConfigProviderEnabled.StaticConfig} to indicate that the configuration
 * provider should gather this configuration at journey creation time.
 */
@Supported
@Target(TYPE)
@Retention(RUNTIME)
public @interface ConfigProviderEnabled {

    /**
     * Indicates that a configuration attribute is required by the {@link OutcomeProvider} at configuration time
     * and will not be provided dynamically at runtime. Usually this is used for configuration attributes that are
     * required to determine the outcomes of the node.
     */
    @Supported
    @Target(METHOD)
    @Retention(RUNTIME)
    @interface StaticConfig { }

}
