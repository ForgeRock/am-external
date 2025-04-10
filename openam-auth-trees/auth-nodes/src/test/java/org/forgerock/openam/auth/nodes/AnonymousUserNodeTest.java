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

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.idm.IdType;

/**
 * Test the anonymous user node.
 */
@ExtendWith(MockitoExtension.class)
public class AnonymousUserNodeTest {

    @Mock
    private AnonymousUserNode.Config config;
    @Mock
    private LegacyIdentityService identityService;

    @Test
    void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("anonymous");
        TreeContext context = new TreeContext(json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        AnonymousUserNode node = new AnonymousUserNode(identityService, config);

        //WHEN
        Action result = node.process(context);

        //THEN
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("anonymous");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn(null);
        TreeContext context = new TreeContext(json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        AnonymousUserNode node = new AnonymousUserNode(identityService, config);

        //WHEN
        assertThatThrownBy(() -> node.process(context))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Anonymous user name could not be found in the configuration");
    }

    @Test
    void processSetTheUsernameInSharedStateWhenNoError() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("anonymous");
        TreeContext context = new TreeContext(json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        AnonymousUserNode node = new AnonymousUserNode(identityService, config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertThat(process.sharedState.isDefined(USERNAME)).isTrue();
        assertThat(process.sharedState.get(USERNAME).asString()).isEqualTo(config.anonymousUserName());
    }

    @Test
    void processReplacesTheUsernameInSharedStateIfOneAlreadyExists() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("anonymous");
        TreeContext context = new TreeContext(json(object(field(USERNAME, "this_user_will_be_replaced"))),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        AnonymousUserNode node = new AnonymousUserNode(identityService, config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertThat(process.sharedState.isDefined(USERNAME)).isTrue();
        assertThat(process.sharedState.get(USERNAME).asString()).isEqualTo(config.anonymousUserName());
    }

    @Test
    void processThrowAnExceptionIfAnonymousUserIsNotSetInTheConfiguration() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("");
        TreeContext context = new TreeContext(json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        AnonymousUserNode node = new AnonymousUserNode(identityService, config);

        //WHEN
        assertThatThrownBy(() -> node.process(context))
                //THEN
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Anonymous user name could not be found in the configuration");
    }


}
