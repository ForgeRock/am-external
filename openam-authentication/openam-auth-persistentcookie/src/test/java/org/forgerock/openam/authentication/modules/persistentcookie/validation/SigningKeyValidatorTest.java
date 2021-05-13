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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.encode.Base64;

public class SigningKeyValidatorTest {
    private SigningKeyValidator testValidator;

    @BeforeMethod
    public void createValidator() {
        testValidator = new SigningKeyValidator();
    }

    @DataProvider(name = "signingKeyData")
    public Object[][] data() {
        return new Object[][] {
            // non Base64 values are invalid
            {Collections.singleton("*&(*£&(&$£$£(**!%£"), false},
            // Value smaller than 256-bit is invalid
            {Collections.singleton(Base64.encode(new byte[31])), false},
            // Correct 256-bit value
            {Collections.singleton(Base64.encode(new byte[32])), true},
            // larger value should still be accepted
            {Collections.singleton(Base64.encode(new byte[33])), true}
        };
    }

    @Test(dataProvider = "signingKeyData")
    public void validate(Set<String> value, boolean expectedResult) {
       assertThat(testValidator.validate(value)).isEqualTo(expectedResult);
    }
}
