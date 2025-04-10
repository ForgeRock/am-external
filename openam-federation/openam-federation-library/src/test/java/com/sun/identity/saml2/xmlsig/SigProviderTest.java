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
 * Copyright 2013-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.xmlsig;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.utils.AMKeyProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;

public class SigProviderTest extends GuiceTestCase {

    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";
    private static final String ID_ATTRIBUTE_VALUE = "signme";

    private static KeyProvider keyProvider = null;
    private static SigProvider sigProvider = null;

    @BeforeAll
    static void setUp() {
        // The keystore properties required to bootstrap this class are setup in the POM
        keyProvider = new AMKeyProvider();
        sigProvider = SigManager.getSigInstance();
    }

    @Test
    void testSigning() throws SAML2Exception {

        String documentToSignXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN)
                    ), "UTF-8");

        // Test the signing of an XML document
        Element signature = sigProvider.sign(documentToSignXML, ID_ATTRIBUTE_VALUE,
                SigningConfigFactory.getInstance()
                        .createXmlSigningConfig(keyProvider.getPrivateKey(DEFAULT_PRIVATE_KEY_ALIAS),
                                keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS)));

        assertThat(signature).isNotNull();
        NodeList nodes = signature.getOwnerDocument().getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    void testVerifySignature() throws SAML2Exception {

        String signedDocumentXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT)
                    ), "UTF-8");

        // Verify that the signed document has a valid signature
        boolean verified = sigProvider.verify(signedDocumentXML, ID_ATTRIBUTE_VALUE,
                Collections.singleton(keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS)));
        assertThat(verified).isTrue();
    }

    @Test
    void testVerifySignatureFromKeyInfo() throws SAML2Exception {
        String signedDocumentXML = XMLUtils.print(
            XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT)), "UTF-8");
        // Verify that the signed document has a valid signature
        boolean verified = sigProvider.verify(signedDocumentXML, ID_ATTRIBUTE_VALUE, null);
        assertThat(verified).isTrue();
    }
}
