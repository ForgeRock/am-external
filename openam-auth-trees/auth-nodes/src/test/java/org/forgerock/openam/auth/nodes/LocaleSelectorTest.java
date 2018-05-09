/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;
import static java.util.Locale.UK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LocaleSelectorTest {


    @DataProvider
    public Object[][] testCases() {
        return new Object[][]{
                {new PreferredLocales(asList(ENGLISH)), asList(UK), null},
                {new PreferredLocales(asList(UK)), asList(UK), UK},
                {new PreferredLocales(asList(UK)), asList(ENGLISH), ENGLISH},
                {new PreferredLocales(asList(UK, FRENCH, ENGLISH)),
                        asList(FRENCH, ENGLISH), FRENCH}
        };
    }

    @Test(dataProvider = "testCases")
    public void matchesCorrectly(PreferredLocales preferredLocales, List<Locale> candidates, Locale expectedOutcome) {
        assertThat(new LocaleSelector().getBestLocale(preferredLocales, candidates)).isEqualTo(expectedOutcome);
    }

}