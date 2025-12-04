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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Map;

import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetSuccessDetailsNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

@ExtendWith(MockitoExtension.class)
class SuccessDetailsTreeHookTest {

    @Mock
    private SetSuccessDetailsNode.Config config;
    @Mock
    private SSOToken ssoToken;
    private JsonValue responseBody;
    private SuccessDetailsTreeHook treeHook;

    @BeforeEach
    void setUp() {
        responseBody = json(object());
        Response response = new Response(Status.OK);
        response.setEntity(responseBody);
        treeHook = new SuccessDetailsTreeHook(config, ssoToken, response);
    }

    @Test
    void testAcceptWillAddStaticSuccessDetails() throws TreeHookException {
        // given
        given(config.successDetails()).willReturn(Map.of("testKey", "testValue"));

        // when
        treeHook.accept();

        // then
        assertThat(responseBody.get("testKey").asString()).isEqualTo("testValue");
    }

    @Test
    void testAcceptWillAddSessionProperties() throws TreeHookException, SSOException {
        // given
        given(config.sessionProperties()).willReturn(Map.of("testKey", "sessionKey"));
        given(ssoToken.getProperty("sessionKey")).willReturn("sessionValue");

        // when
        treeHook.accept();

        // then
        assertThat(responseBody.get("testKey").asString()).isEqualTo("sessionValue");
    }

    @Test
    void testAcceptWillNotAddNullSessionProperty() throws SSOException, TreeHookException {
        // given
        given(config.sessionProperties()).willReturn(Map.of("testKey", "sessionKey"));
        given(ssoToken.getProperty("sessionKey")).willReturn(null);

        // when
        treeHook.accept();

        // then
        assertThat(responseBody.get("testKey").isNull()).isTrue();
    }

    @Test
    void testThrowsTreeHookExceptionWhenResponseBodyUnavailable() throws IOException {
        // given
        Response response = mock(Response.class);
        Entity mockEntity = mock(Entity.class);
        given(response.getEntity()).willReturn(mockEntity);
        given(mockEntity.getJson()).willThrow(new IOException());
        treeHook = new SuccessDetailsTreeHook(config, ssoToken, response);

        // when / then
        assertThatThrownBy(() -> treeHook.accept()).isInstanceOf(TreeHookException.class)
                .hasMessage("Failed to parse response body");
    }

    @Test
    void testThrowsTreeHookExceptionWhenSessionPropertyError() throws SSOException {
        // given
        given(config.sessionProperties()).willReturn(Map.of("testKey", "sessionKey"));
        given(ssoToken.getProperty("sessionKey")).willThrow(new SSOException(""));

        // when / then
        assertThatThrownBy(() -> treeHook.accept()).isInstanceOf(TreeHookException.class)
                .hasMessage("Failed to get property from session testKey");
    }

    @Test
    void testAcceptWillAddStaticJsonObject() throws TreeHookException {
        // given
        given(config.successDetails()).willReturn(Map.of("testKey", "{ \"key\": \"value\" }"));

        // when
        treeHook.accept();

        // then
        assertThat(responseBody.get("testKey").asMap()).containsEntry("key", "value");
    }

}
