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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.discovery;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

/**
 * Configuration for the Discovery war's use of Guice.
 */
public class DiscoveryGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Key.get(new TypeLiteral<ValidDomainExtractor<RedirectUrlValidator.GlobalService>>() {}))
                .to(RelayStateUrlValidator.class);
    }

    @Provides
    @Inject
    RedirectUrlValidator<RedirectUrlValidator.GlobalService> getGlobalRedirectUrlValidator(ValidDomainExtractor<RedirectUrlValidator.GlobalService> urlExtractor) {
        return new RedirectUrlValidator<>(urlExtractor);
    }
}
