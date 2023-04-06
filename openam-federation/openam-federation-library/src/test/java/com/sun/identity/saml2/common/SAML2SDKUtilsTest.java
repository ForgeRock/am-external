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
 * Copyright 2022 ForgeRock AS.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SAML2SDKUtilsTest {
    private static final String HEX_STRING = "37767C58AF6BFD97970DED0BDC320983805B70F6";
    private static final byte[] BYTE_ARRAY = new byte[]{
            55, 118, 124, 88, -81, 107, -3, -105, -105, 13, -19, 11, -36, 50, 9, -125, -128, 91, 112, -10
    };

    @Test
    public void shouldGiveCorrectByteArrayFromString() {
        assertThat(SAML2SDKUtils.hexStringToByteArray(HEX_STRING)).isEqualTo(BYTE_ARRAY);
    }

    @Test
    public void shouldGiveCorrectByteArrayFromStringLowerCase() {
        assertThat(SAML2SDKUtils.hexStringToByteArray(HEX_STRING.toLowerCase())).isEqualTo(BYTE_ARRAY);
    }

    @Test
    public void shouldGiveEmptyByteArrayFromEmptyString() {
        assertThat(SAML2SDKUtils.hexStringToByteArray("")).isEqualTo(new byte[]{});
    }

    @Test
    public void shouldGiveCorrectStringFromByteArray() {
        assertThat(SAML2SDKUtils.byteArrayToHexString(BYTE_ARRAY)).isEqualToIgnoringCase(HEX_STRING);
    }

    @Test
    public void shouldGiveEmptyStringForEmptyByteArray() {
        assertThat(SAML2SDKUtils.byteArrayToHexString(new byte[]{})).isEqualTo("");
    }
}
