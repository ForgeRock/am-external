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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.framework.ModifyAuthLevelNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ModifyAuthLevelNodeTest {

    @Mock
    ModifyAuthLevelNode.Config serviceConfig;

    @Test
    void processIncrementsAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext());
        AssertJJsonValueAssert.assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(15);
    }

    @Test
    void whenSharedStateDoesNotContainAuthLevelProcessIncrementsAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext(json(object())));
        AssertJJsonValueAssert.assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(10);
    }

    @Test
    void processDecrementAuthLevel() throws Exception {
        given(serviceConfig.authLevelIncrement()).willReturn(-10);
        ModifyAuthLevelNode node = new ModifyAuthLevelNode(serviceConfig);
        Action result = node.process(getContext());
        AssertJJsonValueAssert.assertThat(result.sharedState).integerAt(AUTH_LEVEL).isEqualTo(-5);
    }

    private TreeContext getContext() {
        return getContext(json(object(field(AUTH_LEVEL, 5))));
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList(), Optional.empty());
    }
}
