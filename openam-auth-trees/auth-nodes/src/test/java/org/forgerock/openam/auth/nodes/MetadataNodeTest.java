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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;

import com.sun.identity.authentication.spi.MetadataCallback;

public class MetadataNodeTest {

    @Test
    void testProcess() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field("attr1", "some value"),
                field("ignored", "private"),
                field("attr2", array("other value"))
        ));
        JsonValue transientState = json(object(field("attr3", "private")));
        TreeContext context = new TreeContext(sharedState, transientState,
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        MetadataNode.Config config = mock(MetadataNode.Config.class);
        given(config.attributes()).willReturn(asSet("attr1", "attr2", "attr3"));

        // When
        Action result = new MetadataNode(config).process(context);

        // Then
        assertThat(result.sendingCallbacks()).isTrue();
        assertThat(result.callbacks.size()).isEqualTo(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(MetadataCallback.class);
        assertThat(((MetadataCallback) result.callbacks.get(0)).getOutputValue()).isObject()
                .containsOnly(entry("attr1", "some value"), entry("attr2", array("other value")));
    }
}
