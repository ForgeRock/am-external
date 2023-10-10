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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.authentication.modules.windowsdesktopsso;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Guice module containing bindings for  WindowsDesktopSSO.
 */
@AutoService(Module.class)
public class WindowsDesktopSSOModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(WindowsDesktopSSOConfigFactory.class));
    }
}
