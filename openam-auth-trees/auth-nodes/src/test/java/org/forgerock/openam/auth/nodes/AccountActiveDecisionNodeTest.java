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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.service.AMAccountLockout;
import com.sun.identity.idm.AMIdentity;

@ExtendWith(MockitoExtension.class)
public class AccountActiveDecisionNodeTest {

    @Mock
    private AMIdentity mockUser;

    @Mock
    private AMAccountLockout.Factory amAccountLockoutFactory;

    @Mock
    private AMAccountLockout amAccountLockout;

    @Mock
    private Realm realm;

    @Mock
    private NodeUserIdentityProvider identityProvider;

    private AccountActiveDecisionNode accountActiveDecisionNode;
    private TreeContext context;

    @BeforeEach
    void setup() {
        when(identityProvider.getAMIdentity(any(), any())).thenReturn(Optional.of(mockUser));

        context = new TreeContext(retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.of("mockUserId"));

        accountActiveDecisionNode = new AccountActiveDecisionNode(realm, amAccountLockoutFactory, identityProvider);
    }

    @Test
    void shouldReturnTrueIfUserIsNotLockedOut() throws Exception {
        when(amAccountLockoutFactory.create(any())).thenReturn(amAccountLockout);
        when(amAccountLockout.isAccountLocked(any())).thenReturn(false);
        assertThat(accountActiveDecisionNode.process(context).outcome).isEqualTo("true");
    }

    @Test
    void shouldReturnFalseIfUserIsLockedOut() throws Exception {
        when(amAccountLockoutFactory.create(any())).thenReturn(amAccountLockout);
        when(amAccountLockout.isAccountLocked(any())).thenReturn(true);
        assertThat(accountActiveDecisionNode.process(context).outcome).isEqualTo("false");
    }

    @Test
    void shouldThrowNodeExceptionOnMissingUser() {
        context = new TreeContext(json(object(field(USERNAME, "test"))), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        when(identityProvider.getAMIdentity(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountActiveDecisionNode.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Failed to get the identity object");
    }

    private JsonValue retrieveSharedState() {
        return json(object(field(REALM, "realm"), field(USERNAME, "test")));
    }
}
