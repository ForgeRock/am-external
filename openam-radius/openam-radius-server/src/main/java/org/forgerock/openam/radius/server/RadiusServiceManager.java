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
 * Copyright 2015-2019 ForgeRock AS.
 */

package org.forgerock.openam.radius.server;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.radius.server.config.RadiusServerManager;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.common.ShutdownManager;
import com.sun.identity.setup.SetupListener;

/**
 * This class is responsible for starting and stopping the RADIUS server. In <code>AMSetupServlet</code> there is some
 * code that uses the Java Service Loader mechanism to load services that implement AMSetupListener. The
 * <code>com.sun.identity.setup.SetupListener</code> file in this project's resources/META-INF/services folder means
 * this class gets loaded by AMSetupListener as an implementer of SetupListener. As <code>AMSetupServlet</code> loads
 * these services it calls <code>addListener()</code> on each of the services. This is a good mechanism for starting the
 * radius server because at that point it is guaranteed that OpenAM has started. Of course the code submitted by Mark
 * from LDS checks that AM is up and config is available, but this entry point should mean that everything is good to go
 * when that code runs.
 */
public class RadiusServiceManager implements SetupListener, ShutdownListener {

    private static final Logger LOG = LoggerFactory.getLogger(RadiusServiceManager.class);

    private RadiusServerManager radiusServiceManager = null;

    @Override
    public synchronized void setupComplete() {
        LOG.debug("Entering RadiusServiceManager.setupComplete");
        if (radiusServiceManager == null) {
            LOG.debug("No instance of radiusServerManager yet. Obtaining from Guice.");
            radiusServiceManager = InjectorHolder.getInstance(RadiusServerManager.class);
            ShutdownManager.getInstance().addShutdownListener(this);
            radiusServiceManager.startUp();
        } else {
            LOG.warn("addListener called but radiusServiceStarter already exists");
        }
        LOG.debug("Leaving RadiusServiceManager.setupComplete");
    }

    @Override
    public synchronized void shutdown() {
        LOG.debug("Entering RadiusServiceManager.shutdown");
        if (radiusServiceManager != null) {
            radiusServiceManager.shutdown();
            radiusServiceManager = null;
        }
        LOG.debug("Leaving RadiusServiceManager.shutdown");
    }
}
