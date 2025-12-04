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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.guice;

import static com.sun.identity.saml2.common.SAML2Constants.SAML2_CREDENTIAL_RESOLVER_PROPERTY;

import java.util.Optional;
import java.util.Set;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.saml2.plugins.KeyStoreSaml2CredentialResolver;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.forgerock.openam.saml2.soap.SOAPConnectionFactory;
import org.forgerock.openam.saml2.soap.SimpleSOAPConnectionFactory;

import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
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

        bind(SOAPConnectionFactory.class).to(SimpleSOAPConnectionFactory.class);

        expose(Saml2CredentialResolver.class);
        expose(SOAPConnectionFactory.class);
    }

    @Provides
    @Exposed
    public TreeProvider getTreeProvider() {
        return new TreeProvider() {
            @Override
            public Optional<? extends Tree> getTree(Realm realm, String id) {
                return Optional.empty();
            }

            @Override
            public Set<? extends Tree> getTrees(Realm realm) {
                return Set.of();
            }
        }; // fedlet-specific implementation for when default will not be bound
    }
}
