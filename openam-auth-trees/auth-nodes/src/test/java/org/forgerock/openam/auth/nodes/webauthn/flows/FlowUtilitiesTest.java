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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FlowUtilitiesTest {

    @DataProvider (name = "urls")
    public static Object[][] provideStringAndExpectedLength() {
        return new Object[][] {
                { "https://example.com:443/", "https://example.com:443/", true }, //same
                { "https://am.example.com:443/", "https://am.example.com:443/", true }, //same, subrealms
                { "https://example.com:443/", "https://example.com/", true }, //default port omitted
                { "https://example.com/", "https://example.com:443/", true }, //default port omitted
                { "https://example.com", "https://example.com", true }, //default ports omitted
                { "https://example.com", "https://example.com:443/openam", true }, //with a small path

                { "https://example.com:8443", "https://example.com", false }, //unmatched omitted port
                { "http://example.com:443/", "https://example.com:443/", false }, //invalid scheme
                { "https://example.com:443/", "https://test.com:443/", false }, //invalid host
        };
    }

    @Test (dataProvider = "urls")
    public void testCalculateLength(String one, String two, boolean expected) {
        FlowUtilities flowUtils = new FlowUtilities();
        boolean response = flowUtils.originsMatch(one, two);
        assertThat(response).isEqualTo(expected);
    }

}
