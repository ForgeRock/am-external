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
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.authentication.modules.social;

import static com.google.inject.name.Names.named;

import java.io.IOException;

import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;

/**
 * Guice Module for configuring bindings for the Social Auth Modules
 *
 * @since AM 5.5.0
 */
public class SocialAuthGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(named("amSocialAuth")).toInstance(Debug.getInstance("amSocialAuth"));
    }

    @Provides
    @Inject
    @Singleton
    @Named("SocialAuthClientHandler")
    public Handler getHttpClientHandler(ShutdownManager shutdownManager,
            @Named("amSocialAuth") Debug debug) throws HttpApplicationException {
        HttpClientHandler handler = new HttpClientHandler();
        shutdownManager.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdown() {
                try {
                    handler.close();
                } catch (IOException e) {
                    //Ignore, handler may have already been closed.
                    debug.message("Unable to close the HttpClientHandler", e);
                }
            }
        });
        return handler;
    }
}
