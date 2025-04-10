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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.NODE_VERSION_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.PageDecisionOutcomeProvider.PROPERTIES_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.forgerock.am.trees.api.NodeFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;

import com.iplanet.am.util.SystemPropertiesWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageNodePageDecisionOutcomeProviderTest {

    @Mock
    private NodeFactory nodeFactory;
    @Mock
    private Realm realm;
    @Mock
    private SystemPropertiesWrapper systemProperties;

    private PageNode.PageDecisionOutcomeProvider outcomeProvider;

    @BeforeEach
    void setup() throws Exception {
        outcomeProvider = new PageNode.PageDecisionOutcomeProvider(realm, nodeFactory, systemProperties);
    }

    @Test
    void shouldReturnEmptyIfNoNodes() throws Exception {
        assertThat(outcomeProvider.getOutcomes(null, json(object()))).isEmpty();
        Mockito.verifyNoInteractions(nodeFactory);
    }

    @Test
    void shouldReturnEmptyIfNodesEmpty() throws Exception {
        assertThat(outcomeProvider.getOutcomes(null, json(object(field("nodes", array()))))).isEmpty();
        Mockito.verifyNoInteractions(nodeFactory);
    }

    @Test
    void shouldReturnOutcomesForLastNode() throws Exception {
        // Given
        OutcomeProvider childOutcomeProvider = mock(OutcomeProvider.class);
        given(nodeFactory.getOutcomeProvider(realm, "LastNodeType", 1)).willReturn(childOutcomeProvider);
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
        verify(nodeFactory, times(0)).getNodeOutcomes(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnOutcomesForDynamicOutcomesNodes() throws Exception {
        // Given
        given(nodeFactory.getNodeOutcomes(any(), eq(realm), eq("lastNode"), eq("LastNodeType"), eq(1))).willReturn(
                asList(
                        new OutcomeProvider.Outcome("outcome1", "Outcome One"),
                        new OutcomeProvider.Outcome("outcome2", "Outcome Two")
                ));
        PreferredLocales locales = new PreferredLocales();
        JsonValue json = json(object(field("nodes", array(
                object(
                        field(ID_FIELD, "node1"),
                        field(DISPLAY_NAME_FIELD, "Node One"),
                        field(NODE_VERSION_FIELD, 1.0),
                        field(NODE_TYPE_FIELD, "MyNodeType"),
                        field(PROPERTIES_FIELD, object())),
                object(
                        field(ID_FIELD, "lastNode"),
                        field(DISPLAY_NAME_FIELD, "Node Two"),
                        field(NODE_TYPE_FIELD, "LastNodeType"),
                        field(NODE_VERSION_FIELD, 1.0))
        ))));

        // When
        List<OutcomeProvider.Outcome> result = outcomeProvider.getOutcomes(locales, json);

        // Then
        assertThat(result).extracting(outcome -> outcome.id).containsExactly("outcome1", "outcome2");
        verify(nodeFactory).getNodeOutcomes(any(), eq(realm), eq("lastNode"), eq("LastNodeType"), eq(1));
    }
}
