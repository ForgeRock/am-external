/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.fr.oath;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;

/**
 * Guice bindings for the OATH two-step verification module.
 */
public class AuthenticatorOathGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("amAuthOATH")).toInstance(Debug.getInstance("amAuthOATH"));
    }

}
