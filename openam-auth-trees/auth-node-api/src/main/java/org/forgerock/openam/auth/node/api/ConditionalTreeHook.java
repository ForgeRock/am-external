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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

/**
 * A tree hook that can decide whether it should run for the current request.
 * <p>
 * Implementations of this class must be registered with the service loader in order to be discovered, for example via
 * {@code @AutoService(ConditionalTreeHook.class)}.
 */
public interface ConditionalTreeHook extends TreeHook {

    /**
     * Marker config for conditional hooks that do not consume node configuration.
     */
    interface Config {
    }

    /**
     * Determines whether this hook should run for the current tree execution.
     *
     * @return {@code true} if the hook should execute
     */
    boolean shouldExecute();
}
