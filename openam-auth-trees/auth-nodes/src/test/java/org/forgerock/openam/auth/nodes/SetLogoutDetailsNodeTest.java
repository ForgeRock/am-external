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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;

import org.forgerock.openam.auth.logouthook.LogoutHookData;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetLogoutDetailsNodeTest {

    @Mock
    private SetLogoutDetailsNode.Config config;

    @Mock
    private TreeContext context;

    @InjectMocks
    private SetLogoutDetailsNode node;

    @Test
    public void shouldRegisterLogoutHookWithData() throws NodeProcessException {
        Map<String, String> logoutDetails = Map.of("key1", "value1");
        given(config.logoutDetails()).willReturn(logoutDetails);
        List<LogoutHookData> logoutHooks = node.process(context).logoutHooks;
        assertThat(logoutHooks).size().isEqualTo(1);
        assertThat(logoutHooks.get(0).data()).isEqualTo(logoutDetails);
    }
}
