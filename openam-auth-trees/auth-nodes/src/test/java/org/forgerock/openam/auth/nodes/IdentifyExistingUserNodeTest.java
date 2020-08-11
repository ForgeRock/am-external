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
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.IdentifyExistingUserNode.IDM_IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IdentifyExistingUserNodeTest {

    @Mock
    private IdentifyExistingUserNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private IdentityUtils identityUtils;

    private IdentifyExistingUserNode identifyExistingUserNode;

    @DataProvider
    public Object[][] userData() {
        return new Object[][]{
                // Basic User exists
                {
                        json(object(
                                field("_id", "testId"),
                                field("userName", "test"),
                                field("mail", "test@test.com"))
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                                field("_id", "testId"),
                                field("idps", null)
                                )
                        )
                },
                // Social User exists
                {
                        json(object(
                                field("_id", "testId"),
                                field("userName", "test2"),
                                field("mail", "test@test.com"),
                                field("idps", array(
                                        object(
                                                field("_refResourceId", "googleId"),
                                                field("_refResourceCollection", "managed/google")
                                        )))
                                )
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                                field("_id", "testId"),
                                field("idps", array(
                                        object(
                                                field("_refResourceId", "googleId"),
                                                field("_refResourceCollection", "managed/google")
                                        )))
                                )
                        )
                },
                // Social User exists, multiple accounts
                {
                        json(object(
                                field("_id", "testId"),
                                field("userName", "test3"),
                                field("mail", "test@test.com"),
                                field("idps", array(
                                        object(
                                                field("_refResourceId", "googleId"),
                                                field("_refResourceCollection", "managed/google")
                                        ), object(
                                                field("_refResourceId", "amazonId"),
                                                field("_refResourceCollection", "managed/amazon")
                                        )
                                )))
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                                field("_id", "testId"),
                                field("idps", array(
                                        object(
                                                field("_refResourceId", "googleId"),
                                                field("_refResourceCollection", "managed/google")
                                        ), object(
                                                field("_refResourceId", "amazonId"),
                                                field("_refResourceCollection", "managed/amazon")
                                        )
                                )))
                        )
                }
        };
    }

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        identifyExistingUserNode = new IdentifyExistingUserNode(config, realm, identityUtils, idmIntegrationService);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(config.identifier()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @Test
    public void shouldFailIfNoUserExist() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenThrow(
                newResourceException(NOT_FOUND));

        final Action process = identifyExistingUserNode.process(getContext(emptyList(), objectAttributeValues()));

        assertThat(process.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test(dataProvider = "userData")
    public void shouldReturnExpectedOutcome(JsonValue user, JsonValue sharedState, String expectedOutcome,
            JsonValue expectedSharedState)
            throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(user);

        final Action process = identifyExistingUserNode.process(getContext(emptyList(), sharedState));

        assertThat(process.sharedState.get("_id").asString()).isEqualTo(expectedSharedState.get("_id").asString());
        assertThat(process.sharedState.get(IDM_IDPS).getObject()).isEqualTo(
                expectedSharedState.get(IDM_IDPS).getObject());
        assertThat(process.sharedState.isDefined(USERNAME));
        assertThat(process.outcome).isEqualTo(expectedOutcome);

    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private JsonValue objectAttributeValues() {
        return json(object(field(OBJECT_ATTRIBUTES,
                object(
                        field("mail", "test@test.com")
                )
        )));
    }

}
