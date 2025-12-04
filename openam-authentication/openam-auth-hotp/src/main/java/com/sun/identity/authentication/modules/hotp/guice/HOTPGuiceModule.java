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
 * Copyright 2022-2025 Ping Identity Corporation.
 */
package com.sun.identity.authentication.modules.hotp.guice;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl;
import com.sun.identity.authentication.modules.hotp.SMSGateway;

/**
 * Guice module for Auth HOTP.
 */
public class HOTPGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        MapBinder<String, SMSGateway> smsGatewayMapBinder = MapBinder.newMapBinder(binder(), String.class,
                SMSGateway.class);
        smsGatewayMapBinder.addBinding(DefaultSMSGatewayImpl.class.getName()).to(DefaultSMSGatewayImpl.class);
    }
}
