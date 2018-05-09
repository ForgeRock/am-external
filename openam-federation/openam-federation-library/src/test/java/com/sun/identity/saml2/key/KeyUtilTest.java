/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.key;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.metadata.*;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;

public class KeyUtilTest {

    private static final String XML_DOCUMENT_TO_LOAD = "no-use-keydescriptor-metadata.xml";

    /**
     * A test to verify fixing the case where an IDPs metadata contains at least two KeyDescriptor elements
     * without "use" attribute.
     */
    @Test
    public void testNoUseKeyDescriptorEntityDescriptor() throws SAML2MetaException, JAXBException {

        // Load test metadata
        String idpMetadata = XMLUtils.print(
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_LOAD),
                    SAML2Utils.debug), "UTF-8");
        EntityDescriptorElement element = SAML2MetaUtils.getEntityDescriptorElement(idpMetadata);
        List descriptors = element.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (Object descriptor : descriptors) {
            if (descriptor instanceof IDPSSODescriptorElement) {
                KeyDescriptorType type = KeyUtil.getKeyDescriptor((IDPSSODescriptorElement)descriptor, "signing");
                Assert.assertNotNull(type);
                break;
            }
        }
    }
}
