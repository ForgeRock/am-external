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

package com.sun.identity.fedlet;

import java.util.stream.Stream;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.openam.federation.guice.FederationGuiceModule;
import org.forgerock.openam.federation.guice.FedletGuiceModule;
import org.forgerock.openam.plugin.configuration.FedletAuditConfigurationGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;

/**
 * This is a work around to stop Fedlet from loading any Guice modules that are not required.
 * Ideally, the dependencies on such modules should be removed.
 */
public class GuiceModuleConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        InjectorConfiguration.setGuiceModuleLoader(() -> Stream.of(SharedGuiceModule.class,
                FederationGuiceModule.class,
                FedletAuditConfigurationGuiceModule.class,
                FedletGuiceModule.class));
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // Do nothing
    }
}
