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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.FAILURE_URL;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetFailureUrlNodeTest {

    private static final String VALID_URL = "valid-url";
    private static final String OLD_URL = "old-url";
    private static final String NEW_URL = "new-url";

    @Mock
    SetFailureUrlNode.Config serviceConfig;

    @Test
    void processSetsNewFailureUrl() throws Exception {
        given(serviceConfig.failureUrl()).willReturn(VALID_URL);

        Node node = new SetFailureUrlNode(serviceConfig);

        JsonValue beforeSharedState = json(object(field("initial", "initial")));
        Action result = node.process(getContext(beforeSharedState));

        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();

        // result contains old state and new
        AssertJJsonValueAssert.assertThat(result.sharedState).isObject()
                .contains(FAILURE_URL, VALID_URL)
                .contains(entry("initial", "initial"));

        // old state is not modified
        AssertJJsonValueAssert.assertThat(beforeSharedState).isObject().containsExactly(entry("initial", "initial"));
    }

    @Test
    void processSetsNewFailureUrlAsEmpty() throws Exception {
        given(serviceConfig.failureUrl()).willReturn("");

        Node node = new SetFailureUrlNode(serviceConfig);

        JsonValue beforeSharedState = json(object(field("initial", "initial")));
        Action result = node.process(getContext(beforeSharedState));

        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();

        // result contains old state and new
        AssertJJsonValueAssert.assertThat(result.sharedState).isObject()
                .contains(FAILURE_URL, "")
                .contains(entry("initial", "initial"));

        // old state is not modified
        AssertJJsonValueAssert.assertThat(beforeSharedState).isObject().containsExactly(entry("initial", "initial"));
    }

    @Test
    void processSetsNewFailureUrlAsNull() throws Exception {
        given(serviceConfig.failureUrl()).willReturn(null);

        Node node = new SetFailureUrlNode(serviceConfig);

        JsonValue beforeSharedState = json(object(field("initial", "initial")));
        Action result = node.process(getContext(beforeSharedState));

        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();

        // result contains old state and new
        AssertJJsonValueAssert.assertThat(result.sharedState).isObject()
                .contains(FAILURE_URL, null)
                .contains(entry("initial", "initial"));

        // old state is not modified
        AssertJJsonValueAssert.assertThat(beforeSharedState).isObject().containsExactly(entry("initial", "initial"));
    }

    @Test
    void processReplacesExistingFailureUrl() throws Exception {
        given(serviceConfig.failureUrl()).willReturn(NEW_URL);

        Node node = new SetFailureUrlNode(serviceConfig);

        JsonValue beforeSharedState = json(object(field("initial", "initial"), field(FAILURE_URL, OLD_URL)));
        Action result = node.process(getContext(beforeSharedState));

        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();

        // result contains old state and new
        AssertJJsonValueAssert.assertThat(result.sharedState).isObject()
                .contains(FAILURE_URL, NEW_URL)
                .contains(entry("initial", "initial"));

        // old state is not modified
        AssertJJsonValueAssert.assertThat(beforeSharedState).isObject()
                .containsExactly(entry("initial", "initial"), entry(FAILURE_URL, OLD_URL));
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), emptyList(), Optional.empty());
    }
}
