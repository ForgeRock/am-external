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

package org.forgerock.openam.integration.idm.nodes.guice;

import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Guice module for the IDM integration nodes.
 */
@AutoService(Module.class)
public class IdmIntegrationNodesGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<String> treeStateContainers =
                Multibinder.newSetBinder(binder(), String.class, Names.named("treeStateContainers"));
        treeStateContainers.addBinding().toInstance(OBJECT_ATTRIBUTES);
    }
}
