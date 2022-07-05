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
 * Copyright 2017-2021 ForgeRock AS.
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeMetaData;
import org.forgerock.openam.auth.nodes.ChoiceCollectorNode.Config;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class ChoiceCollectorNodeTest {

    private static final int DEFAULT_CHOICE_INDEX = 1;
    Config config;

    @Mock
    TreeMetaData metaData;
    @Mock
    NodeRegistry nodeRegistry;
    @RealmTestHelper.RealmHelper
    static Realm realm;

    ChoiceCollectorNode choiceCollectorNode;

    @BeforeMethod
    public void before() throws NodeProcessException {
        openMocks(this);
        config = whenNodeConfigHasAttributes(2);
    }

    @DataProvider(name = "choices")
    public Object[][] choices() {
        return new Object[][] {
            { 0, "choice0" },
            { 1, "choice1" }
        };
    }

    @Test(dataProvider = "choices")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String choice) throws Exception {
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        choiceCallback.setSelectedIndex(index);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        assertThat(action.outcome).isEqualTo(choice);
    }

    @Test
    public void testProcessWithChoiceCallbackNotSelectedIndexReturnsInitialCallback() throws Exception {
        ChoiceCallback choiceCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);

        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(singletonList(choiceCallback)));

        ChoiceCallback expectedCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        expectedCallback.setSelectedIndex(1);
        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isEqualToComparingFieldByField(expectedCallback);
    }

    @Test
    public void testProcessWithNoCallbacksReturnsInitialCallback() throws Exception {
        choiceCollectorNode = new ChoiceCollectorNode(config, null, null);
        Action action = choiceCollectorNode.process(getContext(emptyList()));
        ChoiceCallback expectedCallback = new ChoiceCallback("prompt", new String[]{"choice0", "choice1"},
                DEFAULT_CHOICE_INDEX, false);
        expectedCallback.setSelectedIndex(DEFAULT_CHOICE_INDEX);

        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isEqualToComparingFieldByField(expectedCallback);
    }

    @Test
    public void testOutcomesInCallbackFilteredByAuthLevel() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        Config config = whenNodeConfigHasAttributes(3);
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
    public void testOutcomesInCallbackFilteredByLeafNode() throws Exception {
        // given
        UUID choiceCollectorNodeId = UUID.randomUUID();

        Config config = whenNodeConfigHasAttributes(3);
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
    public void testOnlyOneOutcomeLeftIsImmediatelyReturnedWithoutCallback() throws Exception {
        //given
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
    public void testReturnedOutcomeFilteredByAuthLevel() throws Exception {
        //when
        UUID choiceCollectorNodeId = UUID.randomUUID();


        Config config = whenNodeConfigHasAttributes(3);

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

    private Config whenNodeConfigHasAttributes(int numberOfChoices) {
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < numberOfChoices; i++) {
            choices.add("choice" + i);
        }
        config = mock(Config.class);
        given(config.choices()).willReturn(choices);
        given(config.defaultChoice()).willReturn("choice1");
        given(config.prompt()).willReturn("prompt");
        return config;
    }

    private TreeContext getContext(List<? extends Callback> callbacks) {
        return getContext(callbacks, json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks, Optional.empty());
    }
}
