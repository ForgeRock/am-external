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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.UserAttributeToAccountNameMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Unit tests for AbstractMultiFactorNode.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractMultiFactorNodeTest {

    private static final String EXPECTED_USERNAME = "expected_username";

    @Mock
    private AMIdentity userIdentity;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractMultiFactorNode node;

    @Before
    public void setup() throws IdRepoException, SSOException {
        when(userIdentity.getAttribute(UserAttributeToAccountNameMapping.USERNAME.toString()))
                .thenReturn(Set.of(EXPECTED_USERNAME));
    }

    @Test
    public void testGetUserAttributeForAccountNameNullUserAttribute() {
        String actualUsername =  node.getUserAttributeForAccountName(userIdentity, null);
        assertThat(actualUsername).isEqualTo(EXPECTED_USERNAME);
    }

    @Test
    public void testGetUserAttributeForAccountNameEmptyUserAttribute() {
        String actualUsername =  node.getUserAttributeForAccountName(userIdentity, "");
        assertThat(actualUsername).isEqualTo(EXPECTED_USERNAME);
    }

    @Test
    public void testGetUserAttributeForAccountNameUsernameUserAttribute() {
        String actualUsername =  node.getUserAttributeForAccountName(userIdentity,
                UserAttributeToAccountNameMapping.USERNAME.toString());
        assertThat(actualUsername).isEqualTo(EXPECTED_USERNAME);
    }

}