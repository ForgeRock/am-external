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

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.xmlsig.SignatureProvider;
import com.sun.identity.saml2.key.KeyUtil;

/**
 * This class defines Guice bindings for federation. The bindings added here will be available for fedlet deployments as
 * well.
 */
public class FederationGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public SignatureProvider getSignatureProvider(Injector injector) throws ReflectiveOperationException {
        String impl = SystemConfigurationUtil.getProperty(SAMLConstants.SIGNATURE_PROVIDER_IMPL_CLASS,
                SAMLConstants.AM_SIGNATURE_PROVIDER);
        return injector.getInstance(Class.forName(impl).asSubclass(SignatureProvider.class));
    }

    @Provides
    @Singleton
    public KeyProvider getKeyProvider() {
        return KeyUtil.getKeyProviderInstance();
    }

    @Provides
    public SOAPConnectionFactory getSOAPConnectionFactory() throws SOAPException {
        return SOAPConnectionFactory.newInstance();
    }

    @Provides
    public MessageFactory getMessageFactory() throws SOAPException {
        return MessageFactory.newInstance();
    }
}
