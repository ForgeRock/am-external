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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FlowUtilitiesTest {

    @DataProvider (name = "urls")
    public static Object[][] provideStringAndExpectedLength() {
        return new Object[][] {
                //same
                { singleton("https://example.com:443/"), "https://example.com:443/", true },
                //same, subrealms
                { singleton("https://am.example.com:443/"), "https://am.example.com:443/", true },
                //default port omitted
                { singleton("https://example.com:443/"), "https://example.com/", true },
                //default port omitted
                { singleton("https://example.com/"), "https://example.com:443/", true },
                //default ports omitted
                { singleton("https://example.com"), "https://example.com", true },
                //with a small path
                { singleton("https://example.com"), "https://example.com:443/openam", true },
                //valid w/ multiple
                { unmodifiableSet(
                        new HashSet<>(asList("https://example.com", "https://am.example.com"))),
                        "https://example.com:443/openam", true },
                //invalid w/ multiple
                { unmodifiableSet(
                        new HashSet<>(asList("https://example.com:8443", "https://am.example.com:8444"))),
                        "https://example.com:443/openam", false },
                //unmatched omitted port
                { singleton("https://example.com:8443"), "https://example.com", false },
                //invalid scheme
                { singleton("http://example.com:443/"), "https://example.com:443/", false },
                //invalid host
                { singleton("https://example.com:443/"), "https://test.com:443/", false },
        };
    }

    @Test (dataProvider = "urls")
    public void testOriginsMatch(Set<String> one, String two, boolean expected) {
        FlowUtilities flowUtils = new FlowUtilities();
        boolean response = flowUtils.originsMatch(one, two);
        assertThat(response).isEqualTo(expected);
    }

}
