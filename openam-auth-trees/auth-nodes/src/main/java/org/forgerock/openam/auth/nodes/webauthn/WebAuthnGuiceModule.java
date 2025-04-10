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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;

import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlowFactory;
import org.forgerock.openam.auth.nodes.webauthn.metadata.DefaultMetadataServiceFactory;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataServiceFactory;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.x509.CertificateUtils;

import com.google.auto.service.AutoService;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;

/**
 * Guice module for webauthn injections.
 */
@AutoService(Module.class)
public class WebAuthnGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                    .build(TrustAnchorValidator.Factory.class));

        install(new FactoryModuleBuilder()
                .build(RegisterFlowFactory.class));

        // Set the DefaultMetadataServiceFactory as the default MetadataServiceFactory allowing it to be overridden
        // by other modules if required.
        OptionalBinder.newOptionalBinder(binder(), MetadataServiceFactory.class)
                .setDefault().to(DefaultMetadataServiceFactory.class);
    }

    /**
     * Provides X.509 (always supported) cert factories.
     *
     * @return the cert factory
     */
    @Provides
    @Named("X.509")
    public CertificateFactory getCertificateFactory() {
        return CertificateUtils.getX509Factory();
    }


    /**
     * Provides PKIX (always supported) cert path valdiators.
     *
     * @return the cert path validator
     */
    @Provides
    @Named("PKIX")
    public CertPathValidator getCertPathValidator() {
        return CertificateUtils.getCertPathValidator();
    }
}
