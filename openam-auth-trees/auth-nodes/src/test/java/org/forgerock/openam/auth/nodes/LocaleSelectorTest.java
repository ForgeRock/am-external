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
 * Copyright 2018 ForgeRock AS.
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