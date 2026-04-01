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
package org.forgerock.openam.auth.nodes.rest;

import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptor;

/**
 * Factory for creating {@link NodeResourceVersionRouter} instances.
 */
public interface NodeResourceVersionRouterFactory {

    /**
     * Creates a new {@link NodeResourceVersionRouter} instance.
     * @param providerV1 The {@link SmsCollectionProvider} to use for V1 and V2 routes
     * @param providerV3 The {@link SmsCollectionProvider} to use for V3 routes
     * @return A new {@link NodeResourceVersionRouter} instance.
     */
    NodeResourceVersionRouter create(SubordinateActionsInterceptor providerV1, NodeVersionProvider providerV3);
}
