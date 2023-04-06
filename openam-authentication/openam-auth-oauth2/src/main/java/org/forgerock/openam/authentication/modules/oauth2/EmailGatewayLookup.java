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
 * Copyright 2022 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import java.util.Map;

import javax.inject.Inject;

/**
 * Lookup for instances of {@link EmailGateway}.
 */
public class EmailGatewayLookup {

    private final Map<String, EmailGateway> emailGatewayMap;

    @Inject
    public EmailGatewayLookup(Map<String, EmailGateway> EmailGatewayMap) {
        this.emailGatewayMap = EmailGatewayMap;
    }

    /**
     * Get the instance of {@link EmailGateway} that matches the input class name.
     *
     * @param implementationClassName the implementation class to get
     * @return an instance of {@link EmailGateway}
     *
     * @throws InstantiationException if there is a problem instantiating the class
     * @throws IllegalAccessException if there is a problem instantiating the class
     * @throws ClassNotFoundException if the class with the input name cannot be found
     */
    public EmailGateway getEmailGateway(String implementationClassName) throws InstantiationException,
                                                        IllegalAccessException, ClassNotFoundException {
        EmailGateway gateway;
        if (emailGatewayMap.containsKey(implementationClassName)) {
            gateway = emailGatewayMap.get(implementationClassName);
        } else {
            gateway = Class.forName(implementationClassName).asSubclass(EmailGateway.class).newInstance();
        }
        return gateway;
    }
}
