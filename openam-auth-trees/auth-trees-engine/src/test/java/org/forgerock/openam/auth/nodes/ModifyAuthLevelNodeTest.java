/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.ModifyAuthLevelNode;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ModifyAuthLevelNodeTest {

    @Mock
    ModifyAuthLevelNode.Config serviceConfig;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processIncrementsAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(15);
    }

    @Test
    public void whenSharedStateDoesNotContainAuthLevelProcessIncrementsAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext(json(object())));
        assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(10);
    }

    @Test
    public void processDecrementAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(-10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext());
        assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(-5);
    }

    private TreeContext getContext() {
        return getContext(json(object(field(AUTH_LEVEL, 5))));
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList());
    }
}
