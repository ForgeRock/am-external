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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.oauth2.OAuth2ClientOriginSearcher;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith(MockitoExtension.class)
public class FlowUtilitiesTest {
    @Captor
    ArgumentCaptor<Map<String, Set<String>>> searchCaptor;
    @RegisterExtension
    public LoggerExtension loggerExtension = new LoggerExtension(AuthenticationFlow.class);
    @Mock
    OAuth2ClientOriginSearcher oAuth2ClientOriginSearcher;
    @Mock
    Realm realm;


    public static Stream<Arguments> origins() {
        return Stream.of(
                // same
                arguments(singleton("https://example.com:443/"), "https://example.com:443/", true),
                // same, subrealms
                arguments(singleton("https://am.example.com:443/"), "https://am.example.com:443/", true),
                // default port omitted
                arguments(singleton("https://example.com:443/"), "https://example.com/", true),
                // default port omitted
                arguments(singleton("https://example.com/"), "https://example.com:443/", true),
                // default ports omitted
                arguments(singleton("https://example.com"), "https://example.com", true),
                // with a small path
                arguments(singleton("https://example.com"), "https://example.com:443/openam", true),
                // valid w/ multiple
                arguments(
                    unmodifiableSet(new HashSet<>(asList("https://example.com", "https://am.example.com"))),
                    "https://example.com:443/openam", true
                ),
                // invalid w/ multiple
                arguments(
                    unmodifiableSet(new HashSet<>(asList("https://example.com:8443", "https://am.example.com:8444"))),
                    "https://example.com:443/openam", false
                ),
                // invalid w/ multiple mobile
                arguments(
                        unmodifiableSet(new HashSet<>(
                                asList("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw",
                                        "ios:bundle-id:com.example.app"))),
                        "ios:bundle-id:com.example.newapp", false
                ),
                // unmatched omitted port
                arguments(singleton("https://example.com:8443"), "https://example.com", false),
                // invalid scheme
                arguments(singleton("http://example.com:443/"), "https://example.com:443/", false),
                // invalid host
                arguments(singleton("https://example.com:443/"), "https://test.com:443/", false),
                // Android Facet id
                // (https://fidoalliance.org/specs/fido-uaf-v1.2-id-20180220/
                // fido-appid-and-facets-v1.2-id-20180220.html#processing-rules-for-appid-and-facetid-assertions)
                arguments(
                    singleton("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw"),
                    "android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw", true
                ),
                // valid w/ multiple support Mobile & Web
                arguments(
                    unmodifiableSet(
                        new HashSet<>(asList("android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw",
                                "https://example.com"
                            )
                        )
                    ),
                    "https://example.com:443/openam", true
                ),
                // valid w/ multiple support Mobile & Web
                arguments(
                    unmodifiableSet(
                            new HashSet<>(asList(
                                    "ios:bundle-id:com.example.app",
                                    "android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw",
                                    "https://example.com"))),
                    "android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw", true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("origins")
    public void testConfiguredOriginsMatch(Set<String> configured, String device, boolean expected) throws Exception {
        // given
        FlowUtilities flowUtils = new FlowUtilities(oAuth2ClientOriginSearcher);

        // when
        boolean response = flowUtils.isOriginValid(realm, configured, device);

        // then
        assertThat(response).isEqualTo(expected);
        if (expected) {
            assertThat(loggerExtension.getMessages(ILoggingEvent::getFormattedMessage)).isEmpty();
        }
    }
}
