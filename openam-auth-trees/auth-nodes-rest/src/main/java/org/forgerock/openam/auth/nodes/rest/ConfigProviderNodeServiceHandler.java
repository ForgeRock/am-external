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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static com.sun.identity.sm.SchemaType.ORGANIZATION;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.api.enums.Stability;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory.ConfigProviderFactory;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptorFactory;
import org.forgerock.openam.core.rest.sms.routing.ServiceHandler;

import com.google.auto.service.AutoService;
import com.sun.identity.sm.ServiceSchema;

/**
 * A service endpoint definition for all the Node services.
 */
@AutoService(ServiceHandler.class)
public class ConfigProviderNodeServiceHandler extends NodeServiceHandler {
    private static final Predicate<String> CONFIG_PROVIDER_HANDLES_FUNCTION = "ConfigProviderNode"::equals;
    private final SmsCollectionProviderFactory configProviderCollectionProviderFactory;

    @Inject
    ConfigProviderNodeServiceHandler(
            @ConfigProviderFactory SmsCollectionProviderFactory configProviderCollectionProviderFactory,
            NodeResourceVersionRouterFactory nodeResourceVersionRouterFactory,
            NodeVersionProviderFactory nodeVersionProviderFactory,
            CoreWrapper coreWrapper, NodeRegistry nodeRegistry,
            SubordinateActionsInterceptorFactory subordinateActionsInterceptorFactory) {
        super(coreWrapper, nodeVersionProviderFactory, nodeResourceVersionRouterFactory, nodeRegistry,
                subordinateActionsInterceptorFactory);
        this.configProviderCollectionProviderFactory = requireNonNull(configProviderCollectionProviderFactory);
    }

    @Override
    public boolean handles(String serviceName) {
        return CONFIG_PROVIDER_HANDLES_FUNCTION.test(serviceName);
    }

    @Override
    protected SmsCollectionProvider getProvider(String nodeName, ServiceSchema instanceSchema,
                                                ServiceSchema rootSchema, Stability stability) {
        return configProviderCollectionProviderFactory.create(
                 instanceSchema, ORGANIZATION, rootSchema, List.of(instanceSchema), nodeName, true, stability);
    }
}
