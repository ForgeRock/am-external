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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.saml2;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import org.forgerock.am.saml2.impl.Saml2SsoResponseUtils;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WriteFederationInformationNodeTest {

    private Node node;
    @Mock
    private Saml2SsoResponseUtils responseUtils;
    @Mock
    private NodeUserIdentityProvider identityProvider;

    @BeforeEach
    void setup() {
        node = new WriteFederationInformationNode(responseUtils, identityProvider);
    }

    @Test
    void shouldFailIfAttributesAreMissing() {
        assertThatThrownBy(() -> node.process(context(object())))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("No user information");
    }

    @Test
    void shouldFailIfNameIdInfoIsMissing() {
        assertThatThrownBy(
            () -> node.process(context(object(field("userInfo", object(field("attributes", object())))))))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("sun-fm-saml2-nameid-info");
    }

    @Test
    void shouldLinkAccountsUsingNameIdInfo() throws Exception {
        node.process(context(object(
                field("username", "username"),
                field("userInfo",
                        object(field("attributes",
                                object(field("sun-fm-saml2-nameid-info", array("name-id")))))))));

        verify(responseUtils).linkAccounts(eq("name-id"), eq("username"));
    }

    private TreeContext context(Map<String, Object> content) {
        return new TreeContext(json(content), new Builder().build(), emptyList(), Optional.of("username"));
    }
}
