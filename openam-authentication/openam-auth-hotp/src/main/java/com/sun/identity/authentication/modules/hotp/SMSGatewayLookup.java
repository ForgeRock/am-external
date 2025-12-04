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
package com.sun.identity.authentication.modules.hotp;

import java.util.Map;

import javax.inject.Inject;

/**
 * Lookup for instances of {@link SMSGateway}.
 */
public class SMSGatewayLookup {

    private final Map<String, SMSGateway> smsGatewayMap;

    @Inject
    public SMSGatewayLookup(Map<String, SMSGateway> smsGatewayMap) {
        this.smsGatewayMap = smsGatewayMap;
    }

    /**
     * Get the instance of {@link SMSGateway} that matches the input class name.
     *
     * @param implementationClassName the implementation class to get
     * @return an instance of {@link SMSGateway}
     *
     * @throws InstantiationException if there is a problem instantiating the class
     * @throws IllegalAccessException if there is a problem instantiating the class
     * @throws ClassNotFoundException if the class with the input name cannot be found
     */
    public SMSGateway getSmsGateway(String implementationClassName) throws InstantiationException,
                        IllegalAccessException, ClassNotFoundException {
        SMSGateway gateway;
        if (smsGatewayMap.containsKey(implementationClassName)) {
            gateway = smsGatewayMap.get(implementationClassName);
        } else {
            gateway = Class.forName(implementationClassName).asSubclass(SMSGateway.class).newInstance();
        }
        return gateway;
    }
}
