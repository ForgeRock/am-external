/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.DISPLAY_NAME_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.ID_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.NODE_TYPE_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.PageDecisionOutcomeProvider.PROPERTIES_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.trees.engine.NodeFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(RealmTestHelper.RealmFixture.class)
public class PageNodePageDecisionOutcomeProviderTest {

    @Mock
    private NodeFactory nodeFactory;
    @RealmTestHelper.RealmHelper
    private static Realm realm;
    private PageNode.PageDecisionOutcomeProvider outcomeProvider;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        outcomeProvider = new PageNode.PageDecisionOutcomeProvider(realm, nodeFactory);
    }

    @Test
    public void shouldReturnEmptyIfNoNodes() throws Exception {
        assertThat(outcomeProvider.getOutcomes(null, json(object()))).isEmpty();
        verifyZeroInteractions(nodeFactory);
    }

    @Test
    public void shouldReturnEmptyIfNodesEmpty() throws Exception {
        assertThat(outcomeProvider.getOutcomes(null, json(object(field("nodes", array()))))).isEmpty();
        verifyZeroInteractions(nodeFactory);
    }

    @Test
    public void shouldReturnOutcomesForLastNode() throws Exception {
        // Given
        OutcomeProvider childOutcomeProvider = mock(OutcomeProvider.class);
        given(nodeFactory.getOutcomeProvider(realm, "LastNodeType")).willReturn(childOutcomeProvider);
        given(childOutcomeProvider.getOutcomes(any(), any())).willReturn(asList(
                new OutcomeProvider.Outcome("outcome1", "Outcome One"),
                new OutcomeProvider.Outcome("outcome2", "Outcome Two")
        ));
        PreferredLocales locales = new PreferredLocales();
        JsonValue lastNodeProperties = json(object(field("_id", "lastNode")));
        JsonValue json = json(object(field("nodes", array(
                object(
                        field(ID_FIELD, "node1"),
                        field(DISPLAY_NAME_FIELD, "Node One"),
                        field(NODE_TYPE_FIELD, "MyNodeType"),
                        field(PROPERTIES_FIELD, object())),
                object(
                        field(ID_FIELD, "lastNode"),
                        field(DISPLAY_NAME_FIELD, "Node Two"),
                        field(NODE_TYPE_FIELD, "LastNodeType"),
                        field(PROPERTIES_FIELD, lastNodeProperties.getObject()))
        ))));

        // When
        List<OutcomeProvider.Outcome> result = outcomeProvider.getOutcomes(locales, json);

        // Then
        assertThat(result).extracting(outcome -> outcome.id).containsExactly("outcome1", "outcome2");
        ArgumentCaptor<JsonValue> propertiesCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(childOutcomeProvider).getOutcomes(eq(locales), propertiesCaptor.capture());
        assertThat(propertiesCaptor.getValue().getObject()).isEqualTo(lastNodeProperties.getObject());
    }
}