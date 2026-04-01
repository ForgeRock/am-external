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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptor;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptorFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodeVersionProviderTest {

    @Mock
    private ActionResponse actionResponse;
    @Mock
    private Context context;
    @Mock
    private UriRouterContext uriRouterContext;
    @Mock
    private SmsCollectionProvider version1Provider;
    @Mock
    private SmsCollectionProvider version2Provider;
    @Mock
    private GeneralActionsHandler generalActionsHandler;
    @Mock
    private SubordinateActionsInterceptorFactory generalActionInterceptorFactory;
    @Mock
    private AuthTreeService authTreeService;
    private AuthTreeNodesFilter authTreeNodesFilter;
    private List<SmsCollectionProvider> versionProviders;

    private NodeVersionProvider nodeVersionProvider;

    @BeforeEach
    void setup() {
        versionProviders = new ArrayList<>();
        versionProviders.add(version1Provider);
        given(version1Provider.getServiceName()).willReturn("UsernameCollectorNode");
        given(version1Provider.getServiceVersion()).willReturn("1.0");
        var v1Resource = Resource.resource()
                                   .read(Read.read().build())
                                   .mvccSupported(false)
                                   .title("Version One")
                                   .build();
        given(version1Provider.getResource()).willReturn(v1Resource);
        given(version1Provider.getI18NName()).willReturn(new LocalizableString("Version One"));

        versionProviders.add(version2Provider);
        given(version2Provider.getServiceName()).willReturn("UsernameCollectorNode");
        given(version2Provider.getServiceVersion()).willReturn("2.0");
        var v2Resource = Resource.resource()
                                 .read(Read.read().build())
                                 .mvccSupported(false)
                                 .title("Version Two")
                                 .build();
        given(version2Provider.getResource()).willReturn(v2Resource);
        given(generalActionInterceptorFactory.create(any(), any()))
                .willAnswer(inv -> new SubordinateActionsInterceptor(inv.getArgument(0),
                        generalActionsHandler));
        authTreeNodesFilter = spy(new AuthTreeNodesFilter(authTreeService));
        nodeVersionProvider = createNodeVersionProvider();
    }

    private NodeVersionProvider createNodeVersionProvider() {
        return createNodeVersionProvider(new Router());
    }

    private NodeVersionProvider createNodeVersionProvider(Router router) {
        return new NodeVersionProvider(versionProviders, generalActionInterceptorFactory,
                generalActionsHandler, authTreeNodesFilter, router);
    }

    @Test
    void testApiReturnsDescription() {
        // when
        var apiDescription = nodeVersionProvider.api(null);

        // then
        assertThat(apiDescription.getId()).isEqualTo("UsernameCollectorNodeCollection");
        assertThat(apiDescription.getVersion()).isEqualTo("1.0");
        assertThat(apiDescription.getPaths().getNames()).containsExactly("", "/1.0", "/2.0");
    }

    @Test
    void testHandleApiRequestReturnsDescription() {
        // Given
        var request = mock(Request.class);
        given(request.getResourcePath()).willReturn("");

        // when
        var apiDescription = nodeVersionProvider.handleApiRequest(null, request);

        // then
        assertThat(apiDescription.getId()).isEqualTo("UsernameCollectorNodeCollection");
        assertThat(apiDescription.getVersion()).isEqualTo("1.0");
        assertThat(apiDescription.getPaths().getNames()).containsExactly("", "/1.0", "/2.0");
    }

    @Test
    void testHandleActionCallsGetType() throws ExecutionException, InterruptedException {
        // given
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("");
        ActionRequest request = Requests.newActionRequest("", "getType");

        // when
        var response = nodeVersionProvider.handleAction(context, request);

        // then
        var type = response.get().getJsonContent();
        assertThat(type.get("_id").asString()).isEqualTo("UsernameCollectorNode");
        assertThat(type.get("versions").asList()).containsExactly("1.0", "2.0");
        assertThat(type.keys()).contains("name");
    }

    @Test
    void testHandleActionCallsGeneralActions() {
        // given
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("");
        ActionRequest request = Requests.newActionRequest("", "getAllTypes");
        given(generalActionsHandler.handles("getAllTypes")).willReturn(true);

        // when
        nodeVersionProvider.handleAction(context, request);

        // then
        verify(generalActionsHandler).handle(eq("getAllTypes"), any(), eq(context), eq(request), any());
    }

    @Test
    void testHandleActionGetsProviderForVersion1() {
        // given
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("1.0");
        given(version1Provider.getType(any())).willReturn(Promises.newResultPromise(actionResponse));
        ActionRequest request = Requests.newActionRequest("1.0", "getType");

        // when
        nodeVersionProvider.handleAction(context, request);

        // then
        verify(version1Provider).getType(any());
    }

    @Test
    void testHandleActionGetsProviderForVersion2() {
        // given
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("2.0");
        given(version2Provider.getType(any())).willReturn(Promises.newResultPromise(actionResponse));
        ActionRequest request = Requests.newActionRequest("2.0", "getType");

        // when
        nodeVersionProvider.handleAction(context, request);

        // then
        verify(version2Provider).getType(any());
    }

    @Test
    void testHandleCreateProviderForVersion2() {
        // given
        CreateRequest request = Requests.newCreateRequest("2.0", json(object()));
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("abc");

        // when
        nodeVersionProvider.handleCreate(context, request);

        // then
        verify(version2Provider).createInstance(any(), any());
    }

    @Test
    void testHandleCreateWhenRemainingUriIsEmpty() {
        // given
        CreateRequest request = Requests.newCreateRequest("1.0", json(object()));
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("");

        // when
        var response = nodeVersionProvider.handleCreate(context, request);

        // then
        assertThatThrownBy(response::getOrThrow).isInstanceOf(BadRequestException.class)
                .hasMessage("Missing node id");
    }

    @Test
    void testHandleCreateForVersionless() {
        // given
        CreateRequest request = Requests.newCreateRequest("", json(object()));
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("abc");

        // when
        var response = nodeVersionProvider.handleCreate(context, request);

        // then
        assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testHandleDeleteProviderForVersion2() {
        // given
        doAnswer(inv -> {
            var context = inv.getArgument(0, Context.class);
            var deleteRequest = inv.getArgument(1, DeleteRequest.class);
            var requestHandler = inv.getArgument(2, RequestHandler.class);
            return requestHandler.handleDelete(context, deleteRequest);
        }).when(authTreeNodesFilter).filterDelete(any(), any(), any());
        DeleteRequest request = Requests.newDeleteRequest("2.0/abc");

        // when
        nodeVersionProvider.handleDelete(context, request);

        // then
        verify(version2Provider).deleteInstance(any(), eq("abc"));
    }

    @Test
    void testHandleReadProviderForVersion1() {
        // given
        ReadRequest request = Requests.newReadRequest("1.0/abc");

        // when
        nodeVersionProvider.handleRead(context, request);

        // then
        verify(version1Provider).readInstance(any(), eq("abc"));
    }

    @Test
    void doesNotAllowCreationWithMultipleServiceNames() {
        var versionProvider1 = mock(SmsCollectionProvider.class);
        given(versionProvider1.getServiceName()).willReturn("ServiceName1");
        var versionProvider2 = mock(SmsCollectionProvider.class);
        given(versionProvider2.getServiceName()).willReturn("ServiceName2");
        var versionProviders = List.of(versionProvider1, versionProvider2);

        assertThatThrownBy(() -> new NodeVersionProvider(versionProviders, generalActionInterceptorFactory,
                generalActionsHandler, authTreeNodesFilter, new Router()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCallGetTypeOnHighestVersionHandlerWhenGetLatestType() {
        // Given
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(uriRouterContext.getRemainingUri()).willReturn("");
        given(version2Provider.getType(any())).willReturn(Promises.newResultPromise(actionResponse));
        given(actionResponse.getJsonContent()).willReturn(json(object()));

        // When
        nodeVersionProvider.handleAction(context, Requests.newActionRequest("", "getLatestType"));

        // Then
        verify(version2Provider).getType(any());
        verify(version1Provider, never()).getType(any());

    }

    @Test
    void testHandleApiRequestWithResourcePathDelegatesToRouter() {
        // Given
        var router = mock(Router.class);
        nodeVersionProvider = createNodeVersionProvider(router);
        var request = mock(Request.class);
        given(request.getResourcePath()).willReturn("1.0");

        // When
        nodeVersionProvider.handleApiRequest(null, request);

        // Then
        verify(router).handleApiRequest(null, request);
    }
}
