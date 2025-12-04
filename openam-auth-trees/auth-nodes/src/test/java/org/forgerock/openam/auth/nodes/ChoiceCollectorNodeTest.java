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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.TARGET_AUTH_LEVEL;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeMetaData;
import org.forgerock.openam.auth.nodes.ChoiceCollectorNode.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChoiceCollectorNodeTest {

    private static final int DEFAULT_CHOICE_INDEX = 1;
    @Mock
    Config config;
    ChoiceCollectorNode choiceCollectorNode;
    @Mock
    private TreeMetaData metaData;

    public static Stream<Arguments> choices() {
        return Stream.of(
                arguments(0, "choice0"),
                arguments(1, "choice1")
        );
    }

    @ParameterizedTest
    @MethodSource("choices")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String choice) throws Exception {
        whenNodeHasAttributesNoDefaultOrPrompt(2);
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        choiceCallback.setSelectedIndex(index);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        assertThat(action.outcome).isEqualTo(choice);
    }

    @Test
    void testProcessWithChoiceCallbackNotSelectedIndexReturnsInitialCallback() throws Exception {
        whenNodeConfigHasAttributes(2);
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        ChoiceCallback expectedCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        expectedCallback.setSelectedIndex(1);
        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isInstanceOf(ChoiceCallback.class);
        ChoiceCallback actualCallback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(actualCallback.getChoices()).isEqualTo(expectedCallback.getChoices());
        assertThat(actualCallback.getDefaultChoice()).isEqualTo(expectedCallback.getDefaultChoice());
        assertThat(actualCallback.getPrompt()).isEqualTo(expectedCallback.getPrompt());
        assertThat(actualCallback.getSelectedIndexes()).isEqualTo(expectedCallback.getSelectedIndexes());
        assertThat(actualCallback.allowMultipleSelections()).isEqualTo(expectedCallback.allowMultipleSelections());
    }

    @Test
    void testProcessWithNoCallbacksReturnsInitialCallback() throws Exception {
        whenNodeConfigHasAttributes(2);
        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(emptyList()));
        ChoiceCallback expectedCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        expectedCallback.setSelectedIndex(DEFAULT_CHOICE_INDEX);

        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isInstanceOf(ChoiceCallback.class);
        ChoiceCallback actualCallback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(actualCallback.getChoices()).isEqualTo(expectedCallback.getChoices());
        assertThat(actualCallback.getDefaultChoice()).isEqualTo(expectedCallback.getDefaultChoice());
        assertThat(actualCallback.getPrompt()).isEqualTo(expectedCallback.getPrompt());
        assertThat(actualCallback.getSelectedIndexes()).isEqualTo(expectedCallback.getSelectedIndexes());
        assertThat(actualCallback.allowMultipleSelections()).isEqualTo(expectedCallback.allowMultipleSelections());
    }

    @Test
    void testOutcomesInCallbackFilteredByAuthLevel() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        whenNodeConfigHasAttributes(3);
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice0")).willReturn(Optional.of(4));
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice1")).willReturn(Optional.of(5));
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice2")).willReturn(Optional.of(10));

        choiceCollectorNode = new ChoiceCollectorNode(config, metaData, choiceCollectorNodeId);

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field(AUTH_LEVEL, 5),
                field(TARGET_AUTH_LEVEL, 10)))));

        // then
        ChoiceCallback callback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(callback.getChoices()).isEqualTo(new String[]{"choice1", "choice2"});
        assertThat(callback.getDefaultChoice()).isEqualTo(0);
    }

    @Test
    void testOutcomesInCallbackFilteredByLeafNode() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        whenNodeConfigHasAttributes(3);
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice0")).willReturn(Optional.empty());
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice1")).willReturn(Optional.of(2));
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice2")).willReturn(Optional.of(3));

        choiceCollectorNode = new ChoiceCollectorNode(config, metaData, choiceCollectorNodeId);

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field("authLevel", 11),
                field("targetAuthLevel", 10)))));

        // then
        ChoiceCallback callback = (ChoiceCallback) action.callbacks.get(0);
        assertThat(callback.getChoices()).isEqualTo(new String[]{"choice1", "choice2"});
        assertThat(callback.getDefaultChoice()).isEqualTo(0);
    }

    @Test
    void testOnlyOneOutcomeLeftIsImmediatelyReturnedWithoutCallback() throws Exception {
        //given
        whenNodeHasAttributesNoDefaultOrPrompt(2);
        UUID choiceCollectorNodeId = UUID.randomUUID();

        choiceCollectorNode = new ChoiceCollectorNode(config, metaData, choiceCollectorNodeId);
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice0")).willReturn(Optional.of(4));
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice1")).willReturn(Optional.of(6));

        // when
        Action action = choiceCollectorNode.process(getContext(emptyList(), json(object(
                field("authLevel", 5),
                field("targetAuthLevel", 10)))));

        //then
        assertThat(action.outcome).isEqualTo("choice1");
        assertThat(action.callbacks).isEmpty();
    }

    @Test
    void testReturnedOutcomeFilteredByAuthLevel() throws Exception {
        //when
        UUID choiceCollectorNodeId = UUID.randomUUID();


        whenNodeHasAttributesNoDefaultOrPrompt(3);

        choiceCollectorNode = new ChoiceCollectorNode(config, metaData, choiceCollectorNodeId);
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice0")).willReturn(Optional.of(4));
        given(metaData.getMaxAuthLevel(choiceCollectorNodeId, "choice1")).willReturn(Optional.of(6));

        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice1", "choice2"},
                DEFAULT_CHOICE_INDEX, false);
        choiceCallback.setSelectedIndex(0);

        // given
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback), json(object(
                field(AUTH_LEVEL, 5),
                field(TARGET_AUTH_LEVEL, 10)))));

        // then
        assertThat(action.outcome).isEqualTo("choice1");
    }

    private void whenNodeConfigHasAttributes(int numberOfChoices) {
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < numberOfChoices; i++) {
            choices.add("choice" + i);
        }
        given(config.choices()).willReturn(choices);
        given(config.defaultChoice()).willReturn("choice1");
        given(config.prompt()).willReturn("prompt");
    }

    private void whenNodeHasAttributesNoDefaultOrPrompt(int numberOfChoices) {
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < numberOfChoices; i++) {
            choices.add("choice" + i);
        }
        given(config.choices()).willReturn(choices);
    }

    private TreeContext getContext(List<? extends Callback> callbacks) {
        return getContext(callbacks, json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks, Optional.empty());
    }
}
