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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.validators;

import static com.sun.identity.shared.Constants.AM_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_DEFAULT_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class ReservedCookieValidatorTest {
    @Mock
    private com.iplanet.am.util.SystemPropertiesWrapper systemProperties;

    private ReservedCookieValidator reservedCookieValidator;

    @BeforeEach
    void setup() {
        given(systemProperties.get(com.sun.identity.shared.Constants.AM_COOKIE_NAME, AM_COOKIE_DEFAULT_VALUE))
                .willReturn(AM_COOKIE_DEFAULT_VALUE);
        given(systemProperties.get(com.sun.identity.shared.Constants.AM_LB_COOKIE_NAME, AM_LB_COOKIE_DEFAULT_VALUE))
                .willReturn(AM_LB_COOKIE_DEFAULT_VALUE);
        reservedCookieValidator = new ReservedCookieValidator(systemProperties);
    }

    @Test
    void shouldNotAllowSsoCookieCookies() {
        // given
        var content = Set.of("[Set-Cookie]=iPlanetDirectoryPro=abc; Path=/; HttpOnly",
                "[X-Custom-Header]=value2");
        // when
        boolean isValid = reservedCookieValidator.validate(content);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldNotAllowLbCookieCookies() {
        // given
        var content = Set.of("[Set-Cookie]=amlbcookie=xyz; Path=/; HttpOnly",
                "[X-Custom-Header]=value2");
        // when
        boolean isValid = reservedCookieValidator.validate(content);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldAllowNonReservedCookies() {
        // given
        var content = Set.of("[Set-Cookie]=CustomCookie=123; Path=/; HttpOnly",
                "[X-Custom-Header]=value2");
        // when
        boolean isValid = reservedCookieValidator.validate(content);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldAllowEmptyValues() {
        // given
        var content = Set.<String>of();
        // when
        boolean isValid = reservedCookieValidator.validate(content);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldAllowNullValues() {
        // when
        boolean isValid = reservedCookieValidator.validate(null);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldAllowNonCookieHeaders() {
        // given
        var content = Set.of("[X-Custom-Header]=value1", "[X-Another-Header]=value2");

        // when
        boolean isValid = reservedCookieValidator.validate(content);

        // then
        assertThat(isValid).isTrue();
    }
}
