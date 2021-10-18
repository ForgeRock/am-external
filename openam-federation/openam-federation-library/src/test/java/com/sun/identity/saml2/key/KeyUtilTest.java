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
 * Copyright 2013-2019 ForgeRock AS.
 */
package com.sun.identity.saml2.key;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.forgerock.openam.utils.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Utils;
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

    @BeforeMethod
    public void setup() {
        KeyUtil.clear();
    }

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
        List<RoleDescriptorType> descriptors = element.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (RoleDescriptorType descriptor : descriptors) {
            if (descriptor instanceof IDPSSODescriptorType) {
                KeyDescriptorElement type = KeyUtil.getKeyDescriptor(descriptor, "signing");
                Assert.assertNotNull(type);
                break;
            }
        }
    }

    @Test
    public void shouldBuildCorrectEncryptionConfigForRsaOaep11() throws Exception {
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
    public void shouldBuildCorrectEncryptionConfigForRsaOaep() throws Exception {
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