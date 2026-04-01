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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.GET_LATEST_TYPE;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_OUTCOMES;
import static org.forgerock.openam.rest.RestConstants.SCHEMA;
import static org.forgerock.openam.rest.RestConstants.TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodeSubordinateActionsHandlerTest {

    @Mock
    private GeneralActionsHandler generalActionsHandler;

    @InjectMocks
    private NodeSubordinateActionsHandler handler;

    @Mock
    private Context context;
    @Mock
    private ActionRequest request;
    @Mock
    private RequestHandler usernameCollectorHandler;

    @Test
    void shouldHandleActionsThatAreHandledByTheGeneralActionsHandler() {
        String testAction = "testAction";
        given(generalActionsHandler.handles(testAction)).willReturn(true);

        assertThat(handler.handles(testAction)).isTrue();
    }

    @Test
    void shouldHandleListLatestTypes() {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);

        assertThat(handler.handles(listLatestTypes)).isTrue();
    }

    @Test
    void shouldNotHandleUnknownActions() {
        String unknownAction = "unknownAction";
        given(generalActionsHandler.handles(unknownAction)).willReturn(false);

        assertThat(handler.handles(unknownAction)).isFalse();
    }

    @Test
    void shouldDelegateGeneralActionsToGeneralActionHandler() {
        String action = "someAction";
        given(generalActionsHandler.handles(action)).willReturn(true);

        handler.handle(action, Map.of(), context, request, Set.of());

        verify(generalActionsHandler).handle(action, Map.of(), context, request, Set.of());
    }

    @Test
    void shouldReturnNotSupportedExceptionForUnknownActions() {
        String unknownAction = "unknownAction";
        given(generalActionsHandler.handles(unknownAction)).willReturn(false);

        var result = handler.handle(unknownAction, Map.of(), context, request, Set.of());

        assertThatThrownBy(result::getOrThrowIfInterrupted).isInstanceOf(NotSupportedException.class);
    }

    @Test
    void shouldNotDelegateListLatestTypesToGeneralActionHandler() {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);

        handler.handle(listLatestTypes, Map.of(), context, request, Set.of());

        verify(generalActionsHandler, never()).handle(listLatestTypes, Map.of(), context, request, Set.of());
    }


    @Test
    void listLatestTypesShouldReturnAMapContainingAList() throws ResourceException {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);

        var content = handler.handle(listLatestTypes, Map.of(), context, request, Set.of())
                             .getOrThrowIfInterrupted().getJsonContent();

        assertThat(content.isMap()).isTrue();
        assertThat(content.get("result").isList()).isTrue();
    }

    @Test
    void listLatestTypesShouldGetLatestNodeTypeOfInnerHandler() {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);
        given(usernameCollectorHandler.handleAction(any(), any()))
                .willReturn(newActionResponse(json(object())).asPromise());

        handler.handle(listLatestTypes, Map.of("UsernameCollectorNode", usernameCollectorHandler), context, request,
                Set.of());
        ArgumentCaptor<ActionRequest> requestCaptor = ArgumentCaptor.forClass(ActionRequest.class);
        verify(usernameCollectorHandler).handleAction(eq(context), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAction()).isEqualTo("getLatestType");
    }

    @Test
    void listLatestTypesShouldSetResourceVersionOnLatestTypeRequest() {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);
        given(usernameCollectorHandler.handleAction(any(), any()))
                .willReturn(newActionResponse(json(object())).asPromise());
        given(request.getResourceVersion()).willReturn(Version.version(3));

        handler.handle(listLatestTypes, Map.of("UsernameCollectorNode", usernameCollectorHandler), context, request,
                Set.of());
        ArgumentCaptor<ActionRequest> requestCaptor = ArgumentCaptor.forClass(ActionRequest.class);
        verify(usernameCollectorHandler).handleAction(eq(context), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getResourceVersion()).isEqualTo(Version.version(3));
    }


    @Test
    void listLatestTypesShouldIncludeAllTypesInResponseUnlessTheyAreHiddenFromUi() throws ResourceException {
        String listLatestTypes = "listLatestTypes";
        given(generalActionsHandler.handles(listLatestTypes)).willReturn(false);
        given(usernameCollectorHandler.handleAction(any(), any()))
                .willReturn(newActionResponse(json(object(field("this", "thing")))).asPromise());
        given(request.getResourceVersion()).willReturn(Version.version(3));

        Map<String, RequestHandler> handlers = Map.of("UsernameCollectorNode", usernameCollectorHandler,
                "anotherNode", usernameCollectorHandler);
        Set<String> hiddenFromUi = Set.of("anotherNode");
        var response = handler.handle(listLatestTypes, handlers, context, request, hiddenFromUi)
                               .getOrThrowIfInterrupted().getJsonContent().get("result").asList(Map.class);
        assertThat(response).hasSize(1);
        assertThat(response.get(0).get("this")).isEqualTo("thing");
    }

    @Test
    void shouldHandlelistLatestNodeDefinitions() {
        String listLatestNodeDefinitions = "listLatestNodeDefinitions";
        given(generalActionsHandler.handles(listLatestNodeDefinitions)).willReturn(false);

        assertThat(handler.handles(listLatestNodeDefinitions)).isTrue();
    }

    @Test
    void shouldNotDelegatelistLatestNodeDefinitionsToGeneralActionHandler() {
        String listLatestNodeDefinitions = "listLatestNodeDefinitions";
        given(generalActionsHandler.handles(listLatestNodeDefinitions)).willReturn(false);

        handler.handle(listLatestNodeDefinitions, Map.of(), context, request, Set.of());

        verify(generalActionsHandler, never()).handle(listLatestNodeDefinitions, Map.of(), context, request, Set.of());
    }

    @Test
    void listLatestNodeDefinitionsShouldReturnAMapContainingAMap() throws ResourceException {
        String listLatestNodeDefinitions = "listLatestNodeDefinitions";
        given(generalActionsHandler.handles(listLatestNodeDefinitions)).willReturn(false);

        var content = handler.handle(listLatestNodeDefinitions, Map.of(), context, request, Set.of())
                .getOrThrowIfInterrupted().getJsonContent();

        assertThat(content.isMap()).isTrue();
        assertThat(content.get("result").isMap()).isTrue();
    }

    @Test
    void listLatestNodeDefinitionsShouldInvokeActionsOnInnerHandler() throws ResourceException {
        String listLatestNodeDefinitions = "listLatestNodeDefinitions";
        given(generalActionsHandler.handles(listLatestNodeDefinitions)).willReturn(false);
        given(usernameCollectorHandler.handleAction(any(), any()))
                .willReturn(newActionResponse(json(object(field("version", "2.0")))).asPromise());

        handler.handle(listLatestNodeDefinitions, Map.of("UsernameCollectorNode", usernameCollectorHandler),
                context, request, Set.of());

        ArgumentCaptor<ActionRequest> requestCaptor = ArgumentCaptor.forClass(ActionRequest.class);
        verify(usernameCollectorHandler, times(4))
                .handleAction(eq(context), requestCaptor.capture());

        List<String> actions = requestCaptor.getAllValues().stream()
                .map(ActionRequest::getAction)
                .collect(Collectors.toList());
        assertThat(actions).containsExactlyInAnyOrder(GET_LATEST_TYPE, SCHEMA, TEMPLATE, LIST_OUTCOMES);
    }

    @Test
    void listLatestNodeDefinitionsShouldIncludeAllNodesInResponseUnlessTheyAreHiddenFromUi() throws ResourceException {
        String listLatestNodeDefinitions = "listLatestNodeDefinitions";
        given(generalActionsHandler.handles(listLatestNodeDefinitions)).willReturn(false);
        given(usernameCollectorHandler.handleAction(any(), any()))
                .willReturn(newActionResponse(json(object(field("some", "data"), field("version", "2.0"))))
                        .asPromise());

        Map<String, RequestHandler> handlers = Map.of("UsernameCollectorNode", usernameCollectorHandler,
                "anotherNode", usernameCollectorHandler);
        Set<String> hiddenFromUi = Set.of("anotherNode");

        var response = handler.handle(listLatestNodeDefinitions, handlers, context, request, hiddenFromUi)
                .getOrThrowIfInterrupted().getJsonContent().get("result").asMap();

        assertThat(response).hasSize(1);
        assertThat(response).containsKey("UsernameCollectorNode");
        assertThat(response.get("UsernameCollectorNode")).isNotNull();
    }
}
