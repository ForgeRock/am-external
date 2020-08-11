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
package org.forgerock.openam.auth.nodes.webauthn;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;

/**
 * Guice module for webauthn injections.
 */
public class WebAuthnGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                    .build(TrustAnchorValidator.Factory.class));
    }

    /**
     * Provides X.509 (always supported) cert factories.
     *
     * @return the cert factory
     */
    @Provides
    @Named("X.509")
    public CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            //never thrown, X.509 always supported on Java
        }

        return null;
    }


    /**
     * Provides PKIX (always supported) cert path valdiators.
     *
     * @return the cert path validator
     */
    @Provides
    @Named("PKIX")
    public CertPathValidator getCertPathValidator() {
        try {
            return CertPathValidator.getInstance("PKIX");
        } catch (NoSuchAlgorithmException e) {
            //never thrown, PKIX always supported on Java
        }

        return null;
    }
}
