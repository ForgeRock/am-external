/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.forgerock.guava.common.collect.ImmutableListMultimap;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ZeroPageLoginNodeTest {

    private ZeroPageLoginNode node;
    @Mock
    private ZeroPageLoginNode.Config config;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(config.usernameHeader()).thenReturn("user");
        when(config.passwordHeader()).thenReturn("pass");
        when(config.allowWithoutReferer()).thenReturn(true);
        when(config.referrerWhiteList()).thenReturn(new HashSet<>());
        node = new ZeroPageLoginNode(config);
    }

    @Test
    public void shouldReturnFalseOutcomeWhenNoHeaders() throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat((Object) result.sharedState).isNull();
    }

    @Test
    public void shouldCaptureUsernameWhenPresent() throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of("user", "fred");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.sharedState).stringAt(USERNAME).isEqualTo("fred");
        assertThat(result.sharedState).doesNotContain(PASSWORD);
    }

    @Test
    public void shouldCapturePasswordWhenPresent() throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of("pass", "secure");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.transientState.get(PASSWORD).getObject()).isEqualTo(("secure"));
        assertThat(result.sharedState).doesNotContain(USERNAME);
    }

    @Test
    public void shouldCaptureUsernameAndPasswordWhenPresent() throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of("pass", "secure", "user", "fred");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.sharedState).stringAt(USERNAME).isEqualTo("fred");
        assertThat(result.transientState.get(PASSWORD).getObject()).isEqualTo("secure");
    }

    @DataProvider
    public Object[][] headers() {
        return new String[][]{{"user"}, {"pass"}};
    }

    @Test(dataProvider = "headers", expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionWhenHeadersAreMultivalued(String header) throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of(header, "value1", header, "value2");
        JsonValue sharedState = json(object());

        // When
        node.process(getContext(headers, sharedState));

        // Then - exception
    }

    private TreeContext getContext(ListMultimap<String, String> headers, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().headers(headers).build(), emptyList());
    }
}
