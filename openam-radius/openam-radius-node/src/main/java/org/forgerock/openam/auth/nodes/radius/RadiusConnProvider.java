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
package org.forgerock.openam.auth.nodes.radius;

import java.io.IOException;
import java.net.SocketException;
import java.util.Set;
import java.util.UUID;

import org.forgerock.openam.radius.common.MessageAuthenticatorCalculator;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Provider class for creating RadiusConn instances with specific configuration.
 */
@Singleton
public class RadiusConnProvider {
    private final MessageAuthenticatorCalculator messageAuthenticatorCalculator;
    private final RadiusMonitorPool radiusMonitorPool;

    /**
     * Constructor for RadiusConnProvider.
     * @param calculator               the MessageAuthenticatorCalculator to use.
     * @param radiusMonitorPool        the RadiusMonitorPool to use.
     */
    @Inject
    public RadiusConnProvider(MessageAuthenticatorCalculator calculator, RadiusMonitorPool radiusMonitorPool) {
        this.messageAuthenticatorCalculator = calculator;
        this.radiusMonitorPool = radiusMonitorPool;
    }

    /**
     * Creates a new RadiusConn instance with the given parameters.
     *
     * @param primaryServers                the set of primary RADIUS servers.
     * @param secondaryServers              the set of secondary RADIUS servers.
     * @param sharedSecret                  the shared secret for RADIUS authentication.
     * @param timeout                       the timeout for the connection.
     * @param requireMessageAuthenticator   whether to require Message-Authenticator attribute in requests.
     * @param nodeId                        the unique identifier for the node.
     * @return a new RadiusConn instance
     * @throws IOException if the connection could not be created
     */
    public RadiusNodeConn createConnection(Set<RADIUSServer> primaryServers, Set<RADIUSServer> secondaryServers,
            String sharedSecret, int timeout, boolean requireMessageAuthenticator, UUID nodeId)
            throws IOException {
        try {
            var authenticator = requireMessageAuthenticator ? messageAuthenticatorCalculator : null;
            return new RadiusNodeConn(primaryServers, secondaryServers, sharedSecret, timeout,
                    authenticator, radiusMonitorPool.get(nodeId));
        } catch (SocketException e) {
            throw new IOException("Failed to create RADIUS connection", e);
        }
    }
}
