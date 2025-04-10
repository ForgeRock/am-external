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
package com.sun.identity.saml2.key;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.forgerock.openam.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.xml.XMLUtils;

public class KeyUtilTest {

    private static final String XML_DOCUMENT_TO_LOAD = "no-use-keydescriptor-metadata.xml";
    private static final String KEY_DESCRIPTOR_XML = "/saml2/key-descriptor.xml";

    @BeforeEach
    void setup() {
        KeyUtil.clear();
    }

    /**
     * A test to verify fixing the case where an IDPs metadata contains at least two KeyDescriptor elements
     * without "use" attribute.
     */
    @Test
    void testNoUseKeyDescriptorEntityDescriptor() throws SAML2MetaException, JAXBException {

        // Load test metadata
        String idpMetadata = XMLUtils.print(
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_LOAD)
                ), "UTF-8");
        EntityDescriptorElement element = SAML2MetaUtils.getEntityDescriptorElement(idpMetadata);
        List<RoleDescriptorType> descriptors = element.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (RoleDescriptorType descriptor : descriptors) {
            if (descriptor instanceof IDPSSODescriptorType) {
                KeyDescriptorElement type = KeyUtil.getKeyDescriptor(descriptor, "signing");
                assertThat(type).isNotNull();
                break;
            }
        }
    }

    @Test
    void shouldBuildCorrectEncryptionConfigForRsaOaep11() throws Exception {
        // Given
        RoleDescriptorType roleDescriptor = new SPSSODescriptorType();
        roleDescriptor.getKeyDescriptor().add(getKeyDescriptor());

        // When
        EncryptionConfig encryptionConfig = KeyUtil.getEncryptionConfig(roleDescriptor, "hello", "world", "/");

        // Then
        assertThat(encryptionConfig).isNotNull();
        assertThat(encryptionConfig.getDataEncAlgorithm()).isEqualTo(XMLCipher.AES_128);
        assertThat(encryptionConfig.getDataEncStrength()).isEqualTo(128);
        assertThat(encryptionConfig.getKeyTransportAlgorithm()).isEqualTo(XMLCipher.RSA_OAEP_11);
        RsaOaepConfig rsaOaepConfig = encryptionConfig.getRsaOaepConfig().get();
        assertThat(rsaOaepConfig.getDigestMethod()).isEqualTo(XMLCipher.SHA512);
        assertThat(rsaOaepConfig.getMaskGenerationFunction()).isEqualTo(EncryptionConstants.MGF1_SHA512);
        assertThat(rsaOaepConfig.getOaepParams()).isEqualTo(Base64.getDecoder().decode("9lWu3Q=="));
    }

    @Test
    void shouldBuildCorrectEncryptionConfigForRsaOaep() throws Exception {
        // Given
        RoleDescriptorType roleDescriptor = new SPSSODescriptorType();
        KeyDescriptorElement keyDescriptor = getKeyDescriptor();
        //remove RSA_OAEP_11 so that RSA_OAEP can be picked up
        keyDescriptor.getValue().getEncryptionMethod().remove(1);
        roleDescriptor.getKeyDescriptor().add(keyDescriptor);

        // When
        EncryptionConfig encryptionConfig = KeyUtil.getEncryptionConfig(roleDescriptor, "hello", "world", "/");

        // Then
        assertThat(encryptionConfig).isNotNull();
        assertThat(encryptionConfig.getDataEncAlgorithm()).isEqualTo(XMLCipher.AES_128);
        assertThat(encryptionConfig.getDataEncStrength()).isEqualTo(128);
        assertThat(encryptionConfig.getKeyTransportAlgorithm()).isEqualTo(XMLCipher.RSA_OAEP);
        RsaOaepConfig rsaOaepConfig = encryptionConfig.getRsaOaepConfig().get();
        assertThat(rsaOaepConfig.getDigestMethod()).isEqualTo(XMLCipher.SHA512);
        assertThat(rsaOaepConfig.getMaskGenerationFunction()).isEqualTo(EncryptionConstants.MGF1_SHA1);
        assertThat(rsaOaepConfig.getOaepParams()).isEqualTo(Base64.getDecoder().decode("9lWu3Q=="));
    }

    private KeyDescriptorElement getKeyDescriptor() throws Exception {
        return (KeyDescriptorElement) SAML2MetaUtils.convertStringToJAXB(
                IOUtils.getFileContentFromClassPath(KeyUtilTest.class, KEY_DESCRIPTOR_XML));
    }
}
