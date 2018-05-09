/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.xmlsig;


import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.utils.AMKeyProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collections;

public class SigProviderTest {

    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";
    private static final String ID_ATTRIBUTE_VALUE = "signme";

    private KeyProvider keyProvider = null;
    private SigProvider sigProvider = null;

    @BeforeClass
    public void setUp() {

        // The keystore properties required to bootstrap this class are setup in the POM
        keyProvider = new AMKeyProvider();
        sigProvider = SigManager.getSigInstance();
    }

    @Test
    public void testSigning() {

        String documentToSignXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN),
                            SAML2Utils.debug), "UTF-8");

        // Test the signing of an XML document
        Element signature = null;
        try {
            signature = sigProvider.sign(
                    documentToSignXML,
                    ID_ATTRIBUTE_VALUE,
                    keyProvider.getPrivateKey(DEFAULT_PRIVATE_KEY_ALIAS),
                    keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS));
        } catch (SAML2Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(signature);
        NodeList nodes = signature.getOwnerDocument().getElementsByTagName("ds:Signature");
        Assert.assertTrue(nodes.getLength() > 0);
        Assert.assertTrue(signature.isEqualNode(nodes.item(0)));
    }

    @Test
    public void testVerifySignature() {

        String signedDocumentXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT),
                            SAML2Utils.debug), "UTF-8");

        // Verify that the signed document has a valid signature
        boolean verified = false;
        try {
            verified = sigProvider.verify(signedDocumentXML, ID_ATTRIBUTE_VALUE,
                    Collections.singleton(keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS)));
        } catch (SAML2Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(verified);
    }
}
