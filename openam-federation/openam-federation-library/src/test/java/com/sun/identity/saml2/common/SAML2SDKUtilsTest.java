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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.secrets.BasicAuthSecrets;

@ExtendWith(MockitoExtension.class)
public class SAML2SDKUtilsTest {
    @Mock
    private JAXBElement<BaseConfigType> config;
    private static final String HEX_STRING = "37767C58AF6BFD97970DED0BDC320983805B70F6";
    private static final byte[] BYTE_ARRAY = new byte[]{
            55, 118, 124, 88, -81, 107, -3, -105, -105, 13, -19, 11, -36, 50, 9, -125, -128, 91, 112, -10
    };

    private static final String locationUrl = "https://example.com";
    private static final String updatedLocationUrl = "https://user:password@example.com";
    private static final String secretLocationUrl = "https://user:secret@example.com";
    private static final char [] secret = new char[]{'s', 'e', 'c', 'r', 'e', 't'};

    private static final Map<String, List<String>> configMap = Map.of(
            SAML2Constants.BASIC_AUTH_ON,List.of("true"),
            SAML2Constants.BASIC_AUTH_USER,List.of("user"),
            SAML2Constants.BASIC_AUTH_PASSWD,List.of("password")
    );

    private static final Map<String, List<String>> secretConfigMap = Map.of(
            SAML2Constants.BASIC_AUTH_ON,List.of("true"),
            SAML2Constants.BASIC_AUTH_USER,List.of("user"),
            SAML2Constants.SECRET_ID_IDENTIFIER,List.of("test")
    );

    @Test
    void shouldGiveCorrectByteArrayFromString() {
        assertThat(SAML2SDKUtils.hexStringToByteArray(HEX_STRING)).isEqualTo(BYTE_ARRAY);
    }

    @Test
    void shouldGiveCorrectByteArrayFromStringLowerCase() {
        assertThat(SAML2SDKUtils.hexStringToByteArray(HEX_STRING.toLowerCase())).isEqualTo(BYTE_ARRAY);
    }

    @Test
    void shouldGiveEmptyByteArrayFromEmptyString() {
        assertThat(SAML2SDKUtils.hexStringToByteArray("")).isEqualTo(new byte[]{});
    }

    @Test
    void shouldGiveCorrectStringFromByteArray() {
        assertThat(SAML2SDKUtils.byteArrayToHexString(BYTE_ARRAY)).isEqualToIgnoringCase(HEX_STRING);
    }

    @Test
    void shouldGiveEmptyStringForEmptyByteArray() {
        assertThat(SAML2SDKUtils.byteArrayToHexString(new byte[]{})).isEqualTo("");
    }

    @Test
    void shouldReturnOriginalLocationUrlIfConfigIsNull() {
        String result = SAML2SDKUtils.fillInBasicAuthInfo(null, locationUrl, "alpha");
        assertThat(result).isEqualTo(locationUrl);
    }

    @Test
    void shouldReturnUpdatedLocationUrl() {
        MockedStatic<SAML2MetaUtils> mockSamlUtils = createMockSamlUtils(configMap);
        String result = SAML2SDKUtils.fillInBasicAuthInfo(config, locationUrl, "alpha");
        assertThat(result).isEqualTo(updatedLocationUrl);
        mockSamlUtils.close();
    }

    @Test
    void shouldReturnSecretLocationUrlWhenSecretSet() {

        MockedStatic<SAML2MetaUtils> mockSamlUtils = createMockSamlUtils(secretConfigMap);
        try (MockedConstruction<BasicAuthSecrets> mockedBasicAuthSecrets = mockConstruction(BasicAuthSecrets.class,
                (mock, context) -> when(mock.getBasicAuthPassword("alpha","test"))
                        .thenReturn(Optional.of(secret))))
        {
            String result = SAML2SDKUtils.fillInBasicAuthInfo(config, locationUrl, "alpha");
            assertThat(result).isEqualTo(secretLocationUrl);
        }
        mockSamlUtils.close();
    }

    private MockedStatic<SAML2MetaUtils> createMockSamlUtils(Map<String, List<String>> map) {
        MockedStatic<SAML2MetaUtils> mockSamlUtils = mockStatic(SAML2MetaUtils.class);
        mockSamlUtils.when(()-> SAML2MetaUtils.getAttributes(config)).thenReturn(map);
        return mockSamlUtils;
    }

}
