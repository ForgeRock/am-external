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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FlowUtilitiesTest {
    @Captor
    ArgumentCaptor<Map<String, Set<String>>> searchCaptor;
    @Mock
    IdentityUtils identityUtils;
    @Mock
    Realm realm;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @DataProvider
    public static Object[][] origins() {
        return new Object[][]{
                //same
                {singleton("https://example.com:443/"), "https://example.com:443/", true},
                //same, subrealms
                {singleton("https://am.example.com:443/"), "https://am.example.com:443/", true},
                //default port omitted
                {singleton("https://example.com:443/"), "https://example.com/", true},
                //default port omitted
                {singleton("https://example.com/"), "https://example.com:443/", true},
                //default ports omitted
                {singleton("https://example.com"), "https://example.com", true},
                //with a small path
                {singleton("https://example.com"), "https://example.com:443/openam", true},
                //valid w/ multiple
                {unmodifiableSet(
                        new HashSet<>(asList("https://example.com", "https://am.example.com"))),
                        "https://example.com:443/openam", true},
                //invalid w/ multiple
                {unmodifiableSet(
                        new HashSet<>(asList("https://example.com:8443", "https://am.example.com:8444"))),
                        "https://example.com:443/openam", false},
                //unmatched omitted port
                {singleton("https://example.com:8443"), "https://example.com", false},
                //invalid scheme
                {singleton("http://example.com:443/"), "https://example.com:443/", false},
                //invalid host
                {singleton("https://example.com:443/"), "https://test.com:443/", false},
                //Android Facet id
                //(https://fidoalliance.org/specs/fido-uaf-v1.2-id-20180220/
                //fido-appid-and-facets-v1.2-id-20180220.html#processing-rules-for-appid-and-facetid-assertions)
                {singleton("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw"),
                        "android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw", true},
                //valid w/ multiple support Mobile & Web
                {unmodifiableSet(
                        new HashSet<>(asList("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw",
                                "https://example.com"))),
                        "https://example.com:443/openam", true},
                //valid w/ multiple support Mobile & Web
                {unmodifiableSet(
                        new HashSet<>(asList("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw",
                                "https://example.com"))),
                        "android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw", true},
        };
    }

    @Test(dataProvider = "origins")
    public void testConfiguredOriginsMatch(Set<String> configured, String device, boolean expected) throws Exception {
        // given
        given(identityUtils.findActiveIdentity(any(), any(), any(), any())).willReturn(Optional.empty());
        FlowUtilities flowUtils = new FlowUtilities(identityUtils);

        // when
        boolean response = flowUtils.isOriginValid(realm, configured, device);

        // then
        assertThat(response).isEqualTo(expected);
    }
}
