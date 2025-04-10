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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml.assertion;

import static com.sun.identity.saml.assertion.AssertionTestUtil.getSignedWSFedAssertionXml;
import static com.sun.identity.saml.common.SAMLConstants.TAG_ASSERTION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.AbstractModule;
import com.sun.identity.saml.xmlsig.AMSignatureProvider;
import com.sun.identity.saml.xmlsig.JKSKeyProvider;
import com.sun.identity.saml.xmlsig.SignatureProvider;

public class AssertionSigningIgnoreLineBreaksTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
        .installModule(new TestGuiceModule())
        .build();

    private static AMSignatureProvider signatureProvider;

    @BeforeAll
    static void setup() {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        signatureProvider = new AMSignatureProvider();
        signatureProvider.initialize(new JKSKeyProvider());
    }

    @Test
    void testAssertionSigningIgnoreLineBreaks() throws Exception {
        final String signedXml = getSignedWSFedAssertionXml();

        assertThat(signatureProvider.verifyXMLSignature(signedXml, TAG_ASSERTION_ID, "defaultkey")).isTrue();
        assertThat(signedXml.contains("\n")).isFalse();
    }

    public static class TestGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(AuditEventPublisher.class).toInstance(mock(AuditEventPublisher.class));
            bind(SignatureProvider.class).toInstance(signatureProvider);
        }
    }
}
