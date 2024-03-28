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
 * Copyright 2018-2023 ForgeRock AS.
 */
package com.sun.identity.saml.assertion;

import static com.sun.identity.saml.assertion.AssertionTestUtil.getSignedWSFedAssertionXml;
import static com.sun.identity.saml.common.SAMLConstants.TAG_ASSERTION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.federation.guice.FederationGuiceModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.sun.identity.saml.xmlsig.AMSignatureProvider;
import com.sun.identity.saml.xmlsig.JKSKeyProvider;
import com.sun.identity.saml.xmlsig.SignatureProvider;

@GuiceModules(AssertionSigningTest.TestGuiceModule.class)
public class AssertionSigningTest extends GuiceTestCase {

    private static AMSignatureProvider signatureProvider;

    @BeforeClass
    public void setup() {
        signatureProvider = new AMSignatureProvider();
        signatureProvider.initialize(new JKSKeyProvider());
    }

    @Test
    public void testAssertionSigning() throws Exception {
        assertThat(signatureProvider.verifyXMLSignature(getSignedWSFedAssertionXml(),
                TAG_ASSERTION_ID, "defaultkey")).isTrue();
    }

    public static class TestGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(AuditEventPublisher.class).toInstance(mock(AuditEventPublisher.class));
            bind(SignatureProvider.class).toInstance(signatureProvider);
        }
    }
}
