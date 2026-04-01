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
package org.forgerock.openam.auth.trees.validation;


import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.forgerock.openam.auth.trees.validation.validators.NodeConfigValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodeConnectionsValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodeCoordinatesValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodeDisplayNameValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodeIdValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodeTypeValidator;
import org.forgerock.openam.auth.trees.validation.validators.NodesGraphValidator;
import org.forgerock.openam.auth.trees.validation.validators.RequestBodyExistsValidator;
import org.forgerock.openam.auth.trees.validation.validators.TreeSchemaValidator;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;

/**
 * Guice module for binding authentication tree validators.
 */
@AutoService(Module.class)
public class AuthTreeValidationGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<Integer, Validator> mapBinder = MapBinder.newMapBinder(binder(), Integer.class, Validator.class);
        mapBinder.addBinding(0).to(RequestBodyExistsValidator.class);
        mapBinder.addBinding(5).to(TreeSchemaValidator.class);
        mapBinder.addBinding(10).to(NodeDisplayNameValidator.class);
        mapBinder.addBinding(20).to(NodeIdValidator.class);
        mapBinder.addBinding(30).to(NodeCoordinatesValidator.class);
        mapBinder.addBinding(40).to(NodeTypeValidator.class);
        mapBinder.addBinding(50).to(NodesGraphValidator.class);
        mapBinder.addBinding(60).to(NodeConfigValidator.class);
        mapBinder.addBinding(70).to(NodeConnectionsValidator.class);
    }

    /**
     * Provides an ordered list of validators based on their @Order annotation.
     * @param validators the set of validators to be ordered
     * @return a list of validators ordered by their @Order annotation
     */
    @Provides
    @Singleton
    List<Validator> orderedValidators(Map<Integer, Validator> validators) {
        return validators.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }
}
