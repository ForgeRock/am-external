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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.services.push;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.services.push.dispatch.handlers.AuthClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.handlers.RegClusterMessageHandler;

/**
 * The abstract push notification delegate contains cluster-wide handlers compatible with the
 * Authenticator Push AM Login module.
 *
 * It has pre-registered handlers for the {@link DefaultMessageTypes}.
 */
public abstract class AbstractPushNotificationDelegate implements PushNotificationDelegate {

    private final Map<MessageType, ClusterMessageHandler> defaultHandlerMap;

    /**
     * Construct a new AbstractPushNotificationDelegate.
     */
    public AbstractPushNotificationDelegate() {
        AuthClusterMessageHandler authMessageHandler = InjectorHolder.getInstance(AuthClusterMessageHandler.class);
        RegClusterMessageHandler regMessageHandler = InjectorHolder.getInstance(RegClusterMessageHandler.class);

        HashMap<MessageType, ClusterMessageHandler> map = new HashMap<>();
        map.put(DefaultMessageTypes.REGISTER, regMessageHandler);
        map.put(DefaultMessageTypes.AUTHENTICATE, authMessageHandler);
        this.defaultHandlerMap = Collections.unmodifiableMap(map);
    }

    @Override
    public Map<MessageType, ClusterMessageHandler> getMessageHandlers() {
        return defaultHandlerMap;
    }
}
