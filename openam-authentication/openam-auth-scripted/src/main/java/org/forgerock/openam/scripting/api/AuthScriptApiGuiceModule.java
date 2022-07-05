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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api;

import static org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory.SCRIPTING_HTTP_CLIENT_NAME;

import org.forgerock.http.Client;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.openam.authentication.service.AuthModuleScriptContextProvider;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.api.http.GroovyHttpClient;
import org.forgerock.openam.scripting.api.http.JavaScriptHttpClient;
import org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository;
import org.forgerock.openam.scripting.persistence.config.defaults.ScriptContextDetailsProvider;
import org.forgerock.openam.shared.guice.CloseableHttpClientProvider;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Defines the bindings needed for the APIs available to the scripted module.
 */
@AutoService(Module.class)
public class AuthScriptApiGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(ScriptIdentityRepository.Factory.class));

        bind(ChfHttpClient.class)
                .annotatedWith(Names.named(ScriptingLanguage.JAVASCRIPT.name()))
                .to(JavaScriptHttpClient.class);

        bind(ChfHttpClient.class)
                .annotatedWith(Names.named(ScriptingLanguage.GROOVY.name()))
                .to(GroovyHttpClient.class);

        bind(Client.class)
                .annotatedWith(Names.named(SCRIPTING_HTTP_CLIENT_NAME))
                .toProvider(CloseableHttpClientProvider.class).in(Scopes.SINGLETON);

        Multibinder.newSetBinder(binder(), ScriptContextDetailsProvider.class)
                .addBinding().to(AuthModuleScriptContextProvider.class);
    }
}
