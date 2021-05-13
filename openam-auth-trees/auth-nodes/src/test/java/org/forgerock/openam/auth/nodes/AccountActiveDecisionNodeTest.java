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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AccountActiveDecisionNodeTest {

    @Mock
    private Realm realm;

    @Mock
    private CoreWrapper coreWrapper;

    @Mock
    private AMIdentity mockUser;

    @Mock
    private IdentityUtils identityUtils;

    private AccountActiveDecisionNode accountActiveDecisionNode;
    private TreeContext context;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        when(coreWrapper.getIdentity(eq("mockUserId"))).thenReturn(mockUser);

        context = new TreeContext(retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.of("mockUserId"));

        accountActiveDecisionNode = new AccountActiveDecisionNode(coreWrapper, identityUtils);
    }

    @Test
    public void shouldReturnTrueIfUserIsActive() throws Exception {
        when(mockUser.isActive()).thenReturn(true);
        assertThat(accountActiveDecisionNode.process(context).outcome).isEqualTo("true");
    }

    @Test
    public void shouldReturnFalseIfUserIsInactive() throws Exception {
        when(mockUser.isActive()).thenReturn(false);
        assertThat(accountActiveDecisionNode.process(context).outcome).isEqualTo("false");
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionOnIdRepoException() throws Exception {
        when(mockUser.isActive()).thenThrow(new IdRepoException());
        accountActiveDecisionNode.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionOnSSOException() throws Exception {
        when(mockUser.isActive()).thenThrow(new SSOException(""));
        accountActiveDecisionNode.process(context);
    }

    private JsonValue retrieveSharedState() {
        return json(object(field(USERNAME, "test")));
    }
}
