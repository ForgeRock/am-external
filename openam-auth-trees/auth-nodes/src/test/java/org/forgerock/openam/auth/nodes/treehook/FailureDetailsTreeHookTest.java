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
 * Copyright 2024-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static com.sun.identity.shared.Constants.AM_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_DEFAULT_VALUE;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.forgerock.openam.auth.node.api.TreeFailureResponse;
import org.forgerock.openam.auth.nodes.SetFailureDetailsNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.shared.Constants;

@ExtendWith(MockitoExtension.class)
class FailureDetailsTreeHookTest {

    @Mock
    TreeFailureResponse response;
    @Mock
    SetFailureDetailsNode.Config config;
    @Mock
    SystemPropertiesWrapper systemProperties;

    FailureDetailsTreeHook failureDetailsTreeHook;

    @BeforeEach
    void setUp() {
        given(systemProperties.get(Constants.AM_COOKIE_NAME, AM_COOKIE_DEFAULT_VALUE))
                .willReturn(AM_COOKIE_DEFAULT_VALUE);
        given(systemProperties.get(Constants.AM_LB_COOKIE_NAME, AM_LB_COOKIE_DEFAULT_VALUE))
                .willReturn(AM_LB_COOKIE_DEFAULT_VALUE);
    }

    @Test
    void testAcceptDoesNothing() {
        // given
        var data = json(object());
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);

        // when
        failureDetailsTreeHook.accept();

        // then
        verifyNoInteractions(config);
        verifyNoInteractions(response);
    }

    @Test
    void testAcceptFailureAddsFailureDetails() {
        // given
        var data = json(object());
        given(config.failureDetails()).willReturn(Map.of("key", "value", "key2", "value2"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addFailureDetail("key", "value");
        verify(response).addFailureDetail("key2", "value2");
        verify(response, never()).setCustomFailureMessage(any());
    }

    @Test
    void testAcceptFailureSetsCustomFailureMessage() {
        // given
        var data = json(object(field("message", "custom message")));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response, never()).addFailureDetail(any(), any());
        verify(response).setCustomFailureMessage("custom message");
    }

    @Test
    void testAcceptFailureAddsJsonFailureDetails() {
        // given
        var data = json(object());
        given(config.failureDetails()).willReturn(Map.of("key", "{ \"key\": \"value\" }"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addFailureDetail("key", Map.of("key", "value"));
    }

    @Test
    void testAcceptFailureAddsHeaders() {
        // given
        var data = json(object());
        given(config.headerControl()).willReturn(Map.of("X-Test-Header", "HeaderValue"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addHeader("X-Test-Header", "HeaderValue");
    }

    @Test
    void shouldRejectReservedHeaders() {
        // given
        var data = json(object());
        given(config.headerControl()).willReturn(Map.of("Content-Type", " text/html; charset=utf-8",
                "X-Frame-Options", "DENY",
                "X-Custom-Header", "CustomValue"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response, never()).addHeader("Content-Type", "text/html; charset=utf-8");
        verify(response, never()).addHeader("X-Frame-Options", "DENY");
        verify(response).addHeader("X-Custom-Header", "CustomValue");
    }

    @Test
    void shouldRejectSetCookieWhenSsoCookieCookies() {
        // given
        var data = json(object());
        given(config.headerControl()).willReturn(Map.of("Set-Cookie", "iPlanetDirectoryPro=abc; Path=/; HttpOnly"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response, never()).addHeader(any(), any());
    }

    @Test
    void shouldRejectSetCookieWhenLbCookieCookies() {
        // given
        var data = json(object());
        given(config.headerControl()).willReturn(Map.of("Set-Cookie", "amlbcookie=abc; Path=/; HttpOnly"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response, never()).addHeader(any(), any());
    }

    @Test
    void shouldAllowCustomCookies() {
        // given
        var data = json(object());
        given(config.headerControl()).willReturn(Map.of("Set-Cookie", "CustomCookie=abc; Path=/; HttpOnly"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data, systemProperties);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addHeader("Set-Cookie", "CustomCookie=abc; Path=/; HttpOnly");
    }
}
