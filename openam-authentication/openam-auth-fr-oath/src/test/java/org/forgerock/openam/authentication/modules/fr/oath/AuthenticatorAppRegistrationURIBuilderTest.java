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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.fr.oath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.modules.fr.oath.validators.CodeLengthValidator.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.xml.bind.DatatypeConverter;

import com.sun.identity.idm.AMIdentity;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthenticatorAppRegistrationURIBuilderTest {

    private final String issuer = "ForgeRock";
    private enum OTPType { TOTP, HOTP };

    @DataProvider(name = "data")
    public Object[][] data() {
        java.util.List<Object[]> testData = new java.util.LinkedList<>();
        for (OTPType otpType : OTPType.values()) {
            for (int i = 0; i < 8; i++) {
                String secretHex =  DatatypeConverter.printHexBinary(
                        RandomStringUtils.randomAlphanumeric(MIN_CODE_LENGTH + i).getBytes());
                testData.add(new Object[] {otpType, secretHex});
            }
        }
        return testData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "data")
    public void OTPUrlSecretDoesNotHavePadding(OTPType otpType, String secretHex) throws Exception {

        // Given
        AMIdentity id = mock(AMIdentity.class);
        given(id.getName()).willReturn(RandomStringUtils.randomAlphabetic(10));

        // When
        String uri;
        AuthenticatorAppRegistrationURIBuilder uriBuilder = new AuthenticatorAppRegistrationURIBuilder(id,
                secretHex, MIN_CODE_LENGTH, issuer);
        switch (otpType) {
            case HOTP:
                uri = uriBuilder.getAuthenticatorAppRegistrationUriForHOTP(0);
                break;
            case TOTP:
            default:
                uri = uriBuilder.getAuthenticatorAppRegistrationUriForTOTP(0);
        }
        String secretParam = uri.substring(uri.indexOf("secret="));
        secretParam = secretParam.substring("secret=".length(), secretParam.indexOf("&"));
        byte[] decodedSecret = (new Base32()).decode(secretParam.getBytes());

        // Then
        assertThat(secretParam).doesNotContain("=");
        assertThat(DatatypeConverter.printHexBinary(decodedSecret)).isEqualTo(secretHex);
    }
}

