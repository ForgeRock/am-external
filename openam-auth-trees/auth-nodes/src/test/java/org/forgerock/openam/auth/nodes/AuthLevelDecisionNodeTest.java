/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AuthLevelDecisionNodeTest {

    @Mock
    AuthLevelDecisionNode.Config serviceConfig;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processReturnsTrueOutcomeIfAuthLevelIsEqualToRequirement() throws Exception {
        given(serviceConfig.authLevelRequirement()).willReturn(10);
        Node node = new AuthLevelDecisionNode(serviceConfig);
        Action result = node.process(getContext(json(object(field(AUTH_LEVEL, 10)))));
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void processReturnsTrueOutcomeIfAuthLevelIsGreaterToRequirement() throws Exception {
        given(serviceConfig.authLevelRequirement()).willReturn(10);
        Node node = new AuthLevelDecisionNode(serviceConfig);
        Action result = node.process(getContext(json(object(field(AUTH_LEVEL, 11)))));
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void processReturnsFalseOutcomeIfAuthLevelIsLessToRequirement() throws Exception {
        given(serviceConfig.authLevelRequirement()).willReturn(10);
        Node node = new AuthLevelDecisionNode(serviceConfig);
        Action result = node.process(getContext(json(object(field(AUTH_LEVEL, 9)))));
        assertThat(result.outcome).isEqualTo("false");
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList());
    }
}
