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
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.KbaConfig;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class KbaDecisionNodeTest {
    private static final String KBA_INFO = "kbaInfo";

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    @Mock
    private KbaDecisionNode.Config config;

    private KbaDecisionNode kbaDecisionNode;
    private KbaConfig kbaConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaDecisionNode/idmKbaConfig.json"), KbaConfig.class);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();

        kbaDecisionNode = new KbaDecisionNode(realm, config, idmIntegrationService);
    }

    @DataProvider
    public Object[][] kbaData() {
        return new Object[][]{
                // object is null, expect false outcome
                {json(null), false},
                // "kbaInfo" is null, expect false outcome
                {json(object(field(KBA_INFO, null))), false},
                // "kbaInfo" contains one question but require 2, expect false outcome
                {json(object(field(KBA_INFO, new HashMap<>()))), false},
                // "kbaInfo" contains two question and require 2, expect true outcome
                {json(object(field(KBA_INFO, array(new HashMap<>(), new HashMap<>())))), true},
                // "kbaInfo" contains three question and require 2, expect true outcome
                {json(object(field(KBA_INFO, array(new HashMap<>(), new HashMap<>(), new HashMap<>())))), true}
        };
    }

    @Test(dataProvider = "kbaData")
    public void shouldReturnExpectedValue(JsonValue kbaValue, boolean expectedResult) throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(kbaValue);

        String outcome = kbaDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        kbaDecisionNode.process(getContext(emptyList(), sharedState));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
