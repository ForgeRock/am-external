/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.meta;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import org.testng.annotations.Test;
import org.w3c.dom.Document;


public class SAML2MetaSecurityUtilsTest {

    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";

    @Test
    public void testVerifySignature() throws SAML2Exception {

        Document doc = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT),
                SAML2MetaUtils.debug);
        // The keystore properties required to bootstrap the underlying key provider class are setup in the POM
        SAML2MetaSecurityUtils.verifySignature(doc);
    }
}
