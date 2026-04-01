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
package org.forgerock.openam.auth.trees.rest.guice;

import org.forgerock.openam.auth.trees.rest.AuthTreesCollectionProvider;
import org.forgerock.openam.auth.trees.rest.AuthTreesCollectionProviderFactory;
import org.forgerock.openam.authz.PrivilegeDefinition;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

/**
 * Guice module for binding the Auth Trees REST endpoints.
 */
@AutoService(Module.class)
public class AuthTreesRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(AuthTreesCollectionProvider.class, AuthTreesCollectionProvider.class)
                .build(AuthTreesCollectionProviderFactory.class));

        MapBinder<String, PrivilegeDefinition> crestPrivilegeDefinitions =
                MapBinder.newMapBinder(binder(), String.class, PrivilegeDefinition.class,
                        PrivilegeDefinition.CrestPrivilegeDefinitions.class);
        crestPrivilegeDefinitions.addBinding("validateTree").toInstance(
                PrivilegeDefinition.getInstance("validateTree", PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding("getIds").toInstance(
                PrivilegeDefinition.getInstance("getIds", PrivilegeDefinition.Action.READ));
        crestPrivilegeDefinitions.addBinding("validate").toInstance(
                PrivilegeDefinition.getInstance("validate", PrivilegeDefinition.Action.READ));
    }
}
