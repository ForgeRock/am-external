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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.config;

/**
 * Interface describing how to start and stup the RADIUS server service.
 */
public interface RadiusServer {

    /**
     * Tells the service to shut down. Only returns after all launched threads and thread pools have been shutdown.
     */
    void shutdown();

    /**
     * Launches the Radius Server. May be called more than once if more than one trigger is registered such as a
     * SpringFramework servlet and the ServletContextListener. Only the first call will start the process. All others
     * are ignored.
     */
    void startUp();

}
