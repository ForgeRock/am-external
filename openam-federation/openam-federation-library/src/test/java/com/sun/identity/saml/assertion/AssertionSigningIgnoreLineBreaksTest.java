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
 * Copyright 2018 ForgeRock AS.
 */
package com.sun.identity.saml.assertion;

import static com.sun.identity.saml.assertion.AssertionTestUtil.getSignedWSFedAssertionXml;
import static com.sun.identity.saml.common.SAMLConstants.TAG_ASSERTION_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.saml.xmlsig.AMSignatureProvider;
import com.sun.identity.saml.xmlsig.JKSKeyProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

public class AssertionSigningIgnoreLineBreaksTest {

    private XMLSignatureManager xmlSignatureManager;

    @BeforeClass
    public void setup() {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        xmlSignatureManager = XMLSignatureManager.getInstance(new JKSKeyProvider(), new AMSignatureProvider());
    }

    @Test
    public void testAssertionSigningIgnoreLineBreaks() throws Exception {
        final String signedXml = getSignedWSFedAssertionXml();

        assertThat(xmlSignatureManager.verifyXMLSignature(signedXml, TAG_ASSERTION_ID, "defaultkey")).isTrue();
        assertThat(signedXml.contains("\n")).isFalse();
    }
}