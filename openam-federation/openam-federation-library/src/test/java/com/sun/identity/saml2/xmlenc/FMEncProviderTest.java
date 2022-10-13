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
 * Copyright 2021-2022 ForgeRock AS.
 */

package com.sun.identity.saml2.xmlenc;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Set;

import org.apache.xml.security.encryption.XMLCipher;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.w3c.dom.Element;

import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.RsaOaepConfig;
import com.sun.identity.shared.xml.XMLUtils;

public class FMEncProviderTest {

    private KeyPair keyPair;

    @BeforeClass
    public void generateKeyPair() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @DataProvider
    public Object[][] dataEncryptionAlgorithms() {
        return new Object[][] {
                { XMLCipher.AES_128, 128 },
                { XMLCipher.AES_192, 192 },
                { XMLCipher.AES_256, 256 },
        };
    }

    @Test(dataProvider = "dataEncryptionAlgorithms")
    public void testDataEncryption(String dataEncryptionAlgorithm, int keySize) throws Exception {
        // Given
        FMEncProvider encProvider = new FMEncProvider();
        String xml = "<test><foo/></test>";
        EncryptionConfig config = new EncryptionConfig(keyPair.getPublic(), dataEncryptionAlgorithm, keySize,
                XMLCipher.RSA_OAEP_11, RsaOaepConfig.getDefaultConfigForKeyTransportAlgorithm(XMLCipher.RSA_OAEP_11));

        // When
        Element encrypted = encProvider.encrypt(xml, config, null, "test");
        Element decrypted = encProvider.decrypt(XMLUtils.print(encrypted), Set.of(keyPair.getPrivate()));

        // Then
        assertThat(XMLUtils.print(decrypted)).isEqualToIgnoringWhitespace(xml);
        assertThat(XMLUtils.print(encrypted)).contains(
                "<xenc11:MGF xmlns:xenc11=\"http://www.w3.org/2009/xmlenc11#\"",
                "Algorithm=\"" + RsaOaepConfig.getDefaultConfigForKeyTransportAlgorithm(XMLCipher.RSA_OAEP_11).get()
                        .getMaskGenerationFunction() + "\"");
    }

    @Test(dataProvider = "dataEncryptionAlgorithms")
    public void testDataEncryptionOAEPMGF1SHA1(String dataEncryptionAlgorithm, int keySize) throws Exception {
        // Given
        FMEncProvider encProvider = new FMEncProvider();
        String xml = "<test><foo/></test>";
        EncryptionConfig config = new EncryptionConfig(keyPair.getPublic(), dataEncryptionAlgorithm, keySize,
                XMLCipher.RSA_OAEP, RsaOaepConfig.getDefaultConfigForKeyTransportAlgorithm(XMLCipher.RSA_OAEP));

        // When
        Element encrypted = encProvider.encrypt(xml, config, null, "test");
        Element decrypted = encProvider.decrypt(XMLUtils.print(encrypted), Set.of(keyPair.getPrivate()));

        // Then
        assertThat(XMLUtils.print(decrypted)).isEqualToIgnoringWhitespace(xml);
        assertThat(XMLUtils.print(encrypted)).doesNotContain(
                "<xenc11:MGF xmlns:xenc11=\"http://www.w3.org/2009/xmlenc11#\" " +
                        "Algorithm=\"http://www.w3.org/2009/xmlenc11#mgf1sha1\"/>");
    }
}