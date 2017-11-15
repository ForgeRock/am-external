/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.sun.identity.common.HttpURLConnectionManager;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;

import javax.inject.Singleton;

@GuiceModule
public class OpenIdConnectGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OpenIdResolverCache.class).to(OpenIdResolverCacheImpl.class).in(Scopes.SINGLETON);
    }

    /*
    The OpenIdResolverCacheImpl needs an instance of the OpenIdResolverFactory to do its work. Can't bind the
    OpenIdResovlerFactory directly, as it has neither a no-arg ctor or a ctor with @Inject, so get around this with
    a provider.
     */
    @Provides
    @Singleton
    OpenIdResolverFactory getResolverFactory() {
        return new OpenIdResolverFactory(HttpURLConnectionManager.getReadTimeout(), HttpURLConnectionManager.getConnectTimeout());
    }
}
