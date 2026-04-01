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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.http.routing.DefaultVersionBehaviour.LATEST;
import static org.forgerock.http.routing.DefaultVersionBehaviour.NONE;
import static org.forgerock.http.routing.DefaultVersionBehaviour.OLDEST;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.DefaultVersionBehaviour;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptor;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodeResourceVersionRouterTest {

    private static final Version VERSION_1 = Version.version(1);
    private static final Version VERSION_2 = Version.version(2);
    private static final Version VERSION_3 = Version.version(3);
    private static final Version VERSION_4 = Version.version(4);

    @Mock
    private Context context;
    @Mock
    private ApiVersionRouterContext apiVersionRouterContext;
    @Mock
    private NodeVersionProvider versionsProvider;
    @Mock
    private SmsCollectionProvider nodeProvider;
    @Mock
    private AuthTreeService authTreeService;
    private SubordinateActionsInterceptor interceptor;
    private AuthTreeNodesFilter authTreeNodesFilter;
    private NodeResourceVersionRouter nodeResourceVersionRouter;

    public static Stream<Arguments> nodeProviderTestParams() {
        return Stream.of(
                Arguments.of(VERSION_1, LATEST),
                Arguments.of(VERSION_2, LATEST));
    }

    public static Stream<Arguments> nodeVersionProviderTestParams() {
        return Stream.of(Arguments.of(VERSION_3, OLDEST),
                Arguments.of(null, LATEST));
    }

    public static Stream<Arguments> notFoundTestParams() {
        return Stream.of(
                Arguments.of(VERSION_4, LATEST),
                Arguments.of(null, NONE));
    }

    @BeforeEach
    void setup() {
        interceptor = new SubordinateActionsInterceptor(nodeProvider, new GeneralActionsHandler(null));
        authTreeNodesFilter = spy(new AuthTreeNodesFilter(authTreeService));
    }

    @Nested
    class HandleAction {
        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ActionRequest request = Requests.newActionRequest("test", "getCreatableTypes");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleAction(context, request);

            // then
            verify(nodeProvider).getCreatableTypes(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ActionRequest request = Requests.newActionRequest("test", "getCreatableTypes");
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleAction(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ActionRequest request = Requests.newActionRequest("test", "getCreatableTypes");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleAction(context, request);

            // then
            verify(versionsProvider).handleAction(any(), any());
        }
    }

    @Nested
    class HandleCreate {
        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            CreateRequest request = Requests.newCreateRequest("", json(object()));
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleCreate(context, request);

            // then
            verify(nodeProvider).createInstance(any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            CreateRequest request = Requests.newCreateRequest("", json(object()));
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleCreate(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            CreateRequest request = Requests.newCreateRequest("", json(object()));
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleCreate(context, request);

            // then
            verify(versionsProvider).handleCreate(any(), any());
        }
    }

    @Nested
    class HandleDelete {
        @Mock
        private RealmContext realmContext;
        @Mock
        private Realm realm;

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            doAnswer(inv -> {
                var context = inv.getArgument(0, Context.class);
                var deleteRequest = inv.getArgument(1, DeleteRequest.class);
                var requestHandler = inv.getArgument(2, RequestHandler.class);
                return requestHandler.handleDelete(context, deleteRequest);
            }).when(authTreeNodesFilter).filterDelete(any(), any(), any());

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            DeleteRequest request = Requests.newDeleteRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleDelete(context, request);

            // then
            verify(nodeProvider).deleteInstance(any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            DeleteRequest request = Requests.newDeleteRequest("test");
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleDelete(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            DeleteRequest request = Requests.newDeleteRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleDelete(context, request);

            // then
            verify(versionsProvider).handleDelete(any(), any());
        }
    }

    @Nested
    class HandlePatch {

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            PatchRequest request = Requests.newPatchRequest("test");
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handlePatch(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            PatchRequest request = Requests.newPatchRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handlePatch(context, request);

            // then
            verify(versionsProvider).handlePatch(any(), any());
        }
    }

    @Nested
    class HandleQuery {
        @Mock
        private QueryResourceHandler handler;

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            QueryRequest request = Requests.newQueryRequest("");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleQuery(context, request, handler);

            // then
            verify(nodeProvider).queryCollection(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            QueryRequest request = Requests.newQueryRequest("");
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleQuery(context, request, handler);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            QueryRequest request = Requests.newQueryRequest("");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleQuery(context, request, handler);

            // then
            verify(versionsProvider).handleQuery(any(), any(), any());
        }
    }

    @Nested
    class HandleRead {

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ReadRequest request = Requests.newReadRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleRead(context, request);

            // then
            verify(nodeProvider).readInstance(any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ReadRequest request = Requests.newReadRequest("test");
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleRead(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            ReadRequest request = Requests.newReadRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleRead(context, request);

            // then
            verify(versionsProvider).handleRead(any(), any());
        }
    }

    @Nested
    class HandleUpdate {

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeProviderTestParams")
        void shouldCallNotVersionedNodeProvider(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            UpdateRequest request = Requests.newUpdateRequest("test", json(object()));
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleUpdate(context, request);

            // then
            verify(nodeProvider).updateInstance(any(), any(), any());
        }

        @ParameterizedTest
        @MethodSource(value = "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#notFoundTestParams")
        void shouldGiveNotFoundWhenUnknownVersion(Version version, DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);

            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            UpdateRequest request = Requests.newUpdateRequest("test", json(object()));
            request.setResourceVersion(version);

            // when
            var response = nodeResourceVersionRouter.handleUpdate(context, request);

            // then
            assertThatThrownBy(response::getOrThrow).isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest
        @MethodSource(value =
                "org.forgerock.openam.auth.nodes.rest.NodeResourceVersionRouterTest#nodeVersionProviderTestParams")
        void shouldCallVersionedProviderWhenNodeVersioningEnabled(Version version,
                                                                  DefaultVersionBehaviour defaultVersionBehaviour) {
            // given
            given(context.as(ApiVersionRouterContext.class)).willReturn(Optional.of(apiVersionRouterContext));
            given(apiVersionRouterContext.getDefaultVersionBehaviour()).willReturn(defaultVersionBehaviour);
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            UpdateRequest request = Requests.newUpdateRequest("test", json(object()));
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.handleUpdate(context, request);

            // then
            verify(versionsProvider).handleUpdate(any(), any());
        }
    }

    @Nested
    class HandleApiRequest {
        @Mock
        private ApiProducer<ApiDescription> apiProducer;

        public static Stream<Arguments> unversionedDescribableTestParams() {
            return Stream.of(Arguments.of(VERSION_1),
                    Arguments.of(VERSION_2));
        }

        @BeforeEach
        void setup() {
            given(apiProducer.newChildProducer(any())).willReturn(apiProducer);
        }

        @ParameterizedTest
        @MethodSource(value = "unversionedDescribableTestParams")
        void shouldReturnOldApiDescriptionForVersion(Version version) {
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            Request request = Requests.newReadRequest("test");
            request.setResourceVersion(version);

            // when
            nodeResourceVersionRouter.api(apiProducer);

            // then
            verify(nodeProvider).api(apiProducer);
            verify(versionsProvider, never()).handleApiRequest(any(), any());
        }

        @Test
        void shouldReturnNewApiDescriptionForNewVersion() {
            // given
            nodeResourceVersionRouter = new NodeResourceVersionRouter(interceptor, versionsProvider,
                    authTreeNodesFilter);

            Request request = Requests.newReadRequest("test");
            request.setResourceVersion(VERSION_3);

            // when
            nodeResourceVersionRouter.api(apiProducer);

            // then
            verify(nodeProvider, never()).handleApiRequest(any(), any());
            verify(versionsProvider).api(apiProducer);
        }
    }

}
