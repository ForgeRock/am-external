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
package org.forgerock.openam.federation.guice;

import static com.sun.identity.saml2.common.SAML2Constants.SAML2_CREDENTIAL_RESOLVER_PROPERTY;

import org.forgerock.openam.saml2.plugins.KeyStoreSaml2CredentialResolver;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;

import com.google.inject.PrivateModule;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * This Guice module adds bindings specifically for the fedlet to allow having different product behaviour for server
 * side SAML implementation and fedlet.
 */
public class FedletGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        String className = SystemConfigurationUtil.getProperty(SAML2_CREDENTIAL_RESOLVER_PROPERTY,
                KeyStoreSaml2CredentialResolver.class.getName());
        try {
            Class<? extends Saml2CredentialResolver> credentialResolver = Class.forName(className)
                    .asSubclass(Saml2CredentialResolver.class);
            bind(Saml2CredentialResolver.class).to(credentialResolver);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        expose(Saml2CredentialResolver.class);
    }
}
