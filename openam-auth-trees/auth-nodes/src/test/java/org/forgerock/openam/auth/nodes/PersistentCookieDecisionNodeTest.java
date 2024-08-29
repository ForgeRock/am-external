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
 * Copyright 2023-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtProvider;
import org.forgerock.openam.core.CoreWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sun.identity.idm.IdType;

@RunWith(MockitoJUnitRunner.class)
public class PersistentCookieDecisionNodeTest {

    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private PersistentCookieDecisionNode.Config config;
    @Mock
    private PersistentJwtProvider persistentJwtProvider;
    @Mock
    private PersistentJwtClaimsHandler persistentJwtClaimsHandler;

    private PersistentCookieDecisionNode persistentCookieDecisionNode;

    @Before
    public void setup() {
        persistentCookieDecisionNode = new PersistentCookieDecisionNode(config, coreWrapper, identityService,
                UUID.randomUUID(), persistentJwtProvider, persistentJwtClaimsHandler);
        given(coreWrapper.convertRealmPathToRealmDn(any())).willReturn("ou=config");
        given(config.persistentCookieName()).willReturn("default-cookie");
        given(config.hmacSigningKey()).willReturn(new char[] {});
    }

    @Test
    public void givenUsernameFoundInCookieThenUserIsIdentified()
            throws NodeProcessException, InvalidPersistentJwtException {
        //given
        given(persistentJwtClaimsHandler.getUsername(any(), any())).willReturn("demo");
        ExternalRequestContext externalRequestContext = new ExternalRequestContext.Builder()
                                                                .cookies(Map.of("default-cookie", "value"))
                                                                .build();

        // when
        Action action = persistentCookieDecisionNode.process(new TreeContext(JsonValue.json(JsonValue.object()),
                externalRequestContext, List.of(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(action.identifiedIdentity).contains(new IdentifiedIdentity("demo", IdType.USER));
    }

    @Test
    public void givenNoCookieFoundThenNoUserIsIdentified() throws NodeProcessException {
        //given
        ExternalRequestContext externalRequestContext = new ExternalRequestContext.Builder()
                                                                .build();

        // when
        Action action = persistentCookieDecisionNode.process(new TreeContext(JsonValue.json(JsonValue.object()),
                externalRequestContext, List.of(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("false");
        assertThat(action.identifiedIdentity).isEmpty();
    }

}
