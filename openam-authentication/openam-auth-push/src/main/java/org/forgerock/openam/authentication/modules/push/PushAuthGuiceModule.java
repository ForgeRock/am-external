/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.push;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;

/**
 * The PushAuthGuiceModule class configures the guice framework for the Push Auth module.
 */
public class PushAuthGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("amAuthPush")).toInstance(Debug.getInstance("amAuthPush"));
    }
}
