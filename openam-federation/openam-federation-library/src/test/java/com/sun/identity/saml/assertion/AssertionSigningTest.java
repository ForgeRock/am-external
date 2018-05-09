/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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


public class AssertionSigningTest {

    private XMLSignatureManager xmlSignatureManager;

    @BeforeClass
    public void setup() {
        xmlSignatureManager = XMLSignatureManager.getInstance(new JKSKeyProvider(), new AMSignatureProvider());
    }

    @Test
    public void testAssertionSigning() throws Exception {
        assertThat(xmlSignatureManager.verifyXMLSignature(getSignedWSFedAssertionXml(),
                TAG_ASSERTION_ID, "defaultkey")).isTrue();
    }
}