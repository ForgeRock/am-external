/*
 *  Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 *  Use of this code requires a commercial software license with ForgeRock AS.
 *  or with one of its affiliates. All use shall be exclusively subject
 *  to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.google.inject.name.Names.named;

import org.forgerock.http.Handler;
import org.forgerock.openam.shared.guice.CloseableHttpClientHandlerProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
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
        bind(Handler.class).annotatedWith(Names.named("SocialAuthClientHandler"))
                                     .toProvider(CloseableHttpClientHandlerProvider.class)
                                     .in(Scopes.SINGLETON);
    }

}
