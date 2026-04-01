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
package org.forgerock.openam.auth.nodes.rest.guice;

import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.CONFIG_PROVIDER_SCRIPT;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.GET_UPGRADED_CONFIG;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_OUTCOMES;

import org.forgerock.openam.auth.nodes.rest.AuthTreeNodeCollectionProvider;
import org.forgerock.openam.auth.nodes.rest.ConfigProviderNodeCollectionProvider;
import org.forgerock.openam.auth.nodes.rest.ConfigProviderNodeServiceHandler;
import org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouter;
import org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterFactory;
import org.forgerock.openam.auth.nodes.rest.NodeServiceHandler;
import org.forgerock.openam.auth.nodes.rest.NodeVersionProvider;
import org.forgerock.openam.auth.nodes.rest.NodeVersionProviderFactory;
import org.forgerock.openam.auth.nodes.rest.PageNodeServiceHandler;
import org.forgerock.openam.auth.nodes.rest.PageNodesCollectionProvider;
import org.forgerock.openam.auth.nodes.rest.PageNodesCollectionProviderFactory;
import org.forgerock.openam.authz.PrivilegeDefinition;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory.ConfigProviderFactory;
import org.forgerock.openam.auth.nodes.rest.NodeRestActions;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Guice module for binding the Auth Nodes REST endpoints.
 */
@AutoService(Module.class)
public class AuthNodesRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(NodeResourceVersionRouter.class, NodeResourceVersionRouter.class)
                .build(NodeResourceVersionRouterFactory.class));

        install(new FactoryModuleBuilder()
                .implement(NodeVersionProvider.class, NodeVersionProvider.class)
                .build(NodeVersionProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(PageNodesCollectionProvider.class, PageNodesCollectionProvider.class)
                .build(PageNodesCollectionProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsCollectionProvider.class, AuthTreeNodeCollectionProvider.class)
                .build(Key.get(SmsCollectionProviderFactory.class, Names.named("authTreeNodesHandlerFactory"))));

        install(new FactoryModuleBuilder()
                .implement(SmsCollectionProvider.class, ConfigProviderNodeCollectionProvider.class)
                .build(Key.get(SmsCollectionProviderFactory.class, ConfigProviderFactory.class)));

        var nodeServiceHandlers = Multibinder.newSetBinder(binder(), NodeServiceHandler.class);
        nodeServiceHandlers.addBinding().to(PageNodeServiceHandler.class);
        nodeServiceHandlers.addBinding().to(ConfigProviderNodeServiceHandler.class);

        MapBinder<String, PrivilegeDefinition> crestPrivilegeDefinitions =
                MapBinder.newMapBinder(binder(), String.class, PrivilegeDefinition.class,
                        PrivilegeDefinition.CrestPrivilegeDefinitions.class);

        crestPrivilegeDefinitions.addBinding(LIST_OUTCOMES).toInstance(
                PrivilegeDefinition.getInstance(LIST_OUTCOMES, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(NodeRestActions.VERSION_INFO).toInstance(
                PrivilegeDefinition.getInstance(NodeRestActions.VERSION_INFO, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(NodeRestActions.LIST_LATEST_TYPES).toInstance(
                PrivilegeDefinition.getInstance(NodeRestActions.LIST_LATEST_TYPES, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(NodeRestActions.LIST_LATEST_NODE_DEFINITIONS).toInstance(
                PrivilegeDefinition.getInstance(
                        NodeRestActions.LIST_LATEST_NODE_DEFINITIONS, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(NodeRestActions.GET_LATEST_TYPE).toInstance(
                PrivilegeDefinition.getInstance(NodeRestActions.GET_LATEST_TYPE, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(GET_UPGRADED_CONFIG)
                .toInstance(PrivilegeDefinition.getInstance(GET_UPGRADED_CONFIG, PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding(CONFIG_PROVIDER_SCRIPT)
                .toInstance(PrivilegeDefinition.getInstance("script", PrivilegeDefinition.Action.READ));
    }
}
