/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.fr.oath.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CodeLengthValidatorTest {

    private final CodeLengthValidator validator = new CodeLengthValidator();

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][] {
                {"7", true},
                {"8", true},
                {"-1", false},
                {"2", false},
                {"words", false},
                {"99999", true}
        };
    }

    @Test(dataProvider = "data")
    public void checkCorrectness(String name, boolean expected) {
        //given

        //when
        boolean result = validator.validate(Collections.singleton(name));

        //then
        assertThat(result).isEqualTo(expected);
    }

}
