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

import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.VersionedPath.UNVERSIONED;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourcePath.empty;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.AUTH_TREE_NODES_RESOURCE;
import static org.forgerock.openam.rest.DescriptorUtils.fromResource;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.GET_LATEST_TYPE;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.VERSION_INFO;
import static org.forgerock.openam.rest.RestConstants.GET_TYPE;
import static org.forgerock.util.i18n.LocalizableString.TRANSLATION_KEY_PREFIX;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SubordinateActionsHandler;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptorFactory;
import org.forgerock.openam.i18n.ConcatenatedLocalizableString;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.google.inject.assistedinject.Assisted;

/**
 * A provider for the versions of a node type.
 */
@CollectionProvider(details = @Handler(mvccSupported = false))
public class NodeVersionProvider implements RequestHandler, Describable<ApiDescription, Request> {

    private static final String COLLECTION_KEY_TEMPLATE = TRANSLATION_KEY_PREFIX + "%s#collectionName";
    private final String resourceName;
    private final List<SmsCollectionProvider> versionProviders;
    private final ApiDescription description;
    private final SubordinateActionsHandler subordinateActionsHandler;
    private final Map<String, RequestHandler> handlers;
    private final Router router;

    @Inject
    NodeVersionProvider(@Assisted List<SmsCollectionProvider> versionProviders,
                        SubordinateActionsInterceptorFactory subordinateActionsInterceptorFactory,
                        @Assisted SubordinateActionsHandler subordinateActionsHandler,
                        AuthTreeNodesFilter authTreeNodesFilter, Router router) {
        Reject.ifTrue(versionProviders.stream()
                        .map(SmsCollectionProvider::getServiceName)
                        .distinct().count() != 1,
                "There must be exactly one service name for the provider set");
        this.versionProviders = versionProviders;
        this.subordinateActionsHandler = subordinateActionsHandler;

        this.router = router;
        handlers = new HashMap<>();
        versionProviders.forEach(smsCollectionProvider -> {
            var version = smsCollectionProvider.getServiceVersion();
            var handler = new FilterChain(newHandler(subordinateActionsInterceptorFactory.create(smsCollectionProvider,
                    new GeneralActionsHandler())), authTreeNodesFilter, new VersionedNodeFilter(version));
            handlers.put(version, handler);
            router.addRoute(RoutingMode.STARTS_WITH, uriTemplate(version), handler);
        });
        resourceName = versionProviders.stream().map(SmsCollectionProvider::getServiceName).findFirst()
                               .orElseThrow();
        description = ApiDescription.apiDescription()
                                   .id(resourceName + "Collection")
                                   .version("1.0")
                                   .paths(createPaths(versionProviders).build())
                                   .build();
    }

    private Paths.Builder createPaths(Collection<SmsCollectionProvider> versionProviders) {
        var paths = Paths.paths();
        var versionedPath = VersionedPath.versionedPath()
                .put(UNVERSIONED, Resource.resource()
                        .title(collectionName())
                        .description("Base resource for the " + resourceName)
                        .action(action().name(GET_TYPE).description(localString("getType.description"))
                                .response(fromResource("AuthTreeNodeCollectionProvider.getType.schema.json",
                                        getClass())).build())
                        .action(action().name(VERSION_INFO).description(localString("versionInfo.description"))
                                .response(fromResource("AuthTreeNodeCollectionProvider.versionInfo.schema.json",
                                        getClass())).build())
                        .action(action().name(GET_LATEST_TYPE).description(localString("getLatestType.description"))
                                .response(fromResource("AuthTreeNodeCollectionProvider.getLatestType.schema.json",
                                        getClass())).build())
                        .mvccSupported(false)
                        .build())
                .build();
        paths.put("", versionedPath);
        for (SmsCollectionProvider provider : versionProviders) {
            var path = VersionedPath.versionedPath().put(UNVERSIONED, provider.getResource()).build();
            paths.put(provider.getServiceVersion(), path);
        }
        return paths;
    }

    private LocalizableString collectionName() {
        SmsCollectionProvider collectionProvider = versionProviders.stream().findFirst().orElseThrow();
        return new LocalizableString(COLLECTION_KEY_TEMPLATE.formatted(collectionProvider.getI18NFileName()),
                getClass().getClassLoader(), new ConcatenatedLocalizableString(collectionProvider.getI18NName(),
                    new LocalizableString("Collection"), " "));
    }

    private JsonValue getTypeValue() {
        return json(object(
                field("_id", resourceName),
                field("name", collectionName()),
                field("versions", array(versionProviders.stream()
                                                          .map(SmsCollectionProvider::getServiceVersion)
                                                          .sorted()
                                                          .toArray()))
        ));
    }

    /**
     * Iterates over each of the subroutes within the handler to retrieve the version info for each version of the
     * node, compiling each into a json array.
     * @param context {@link Context}
     * @param request {@link ActionRequest}
     * @return The json array as a promise, or the ResourceException in the failing case.
     */
    private Promise<ActionResponse, ResourceException> getVersionInfo(Context context, ActionRequest request) {
        JsonValue result = json(array());
        var actionRequest = newActionRequest(empty(), VERSION_INFO)
                .setResourceVersion(request.getResourceVersion());

        return Promises.when(handlers.values().stream()
                .map(handler -> handler.handleAction(context, actionRequest))
                .toList())
                .then(actionResponses -> {
                    for (var response : actionResponses) {
                        result.add(response.getJsonContent());
                    }
                    return newActionResponse(result);
                });
    }

    private Promise<ActionResponse, ResourceException> getLatestType(Context context, ActionRequest request) {
        var actionRequest = newActionRequest(empty(), GET_TYPE)
                .setResourceVersion(request.getResourceVersion());
        return handlers.entrySet()
                .stream()
                .max(Comparator.comparing(
                        e -> Version.version(e.getKey())))
                .map(entry -> entry.getValue().handleAction(context, actionRequest)
                        .then(response -> {
                            JsonValue jsonContent = response.getJsonContent();
                            String version = entry.getKey();
                            jsonContent.put("_id", resourceName);
                            jsonContent.put("version", version);
                            return newActionResponse(jsonContent);
                        }))
                .orElse(new NotFoundException().asPromise());
    }

    private LocalizableString localString(String key) {
        return new LocalizableString(AUTH_TREE_NODES_RESOURCE + key, getClass());
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return description;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        if (request.getResourcePath().isEmpty()) {
            return description;
        } else {
            return router.handleApiRequest(context, request);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        String remainingUri = context.asContext(UriRouterContext.class).getRemainingUri();
        if (remainingUri.isEmpty()) {
            String action = request.getAction();
            if (subordinateActionsHandler.handles(action)) {
                return subordinateActionsHandler.handle(action, handlers, context, request, Set.of());
            }
            return switch (action) {
            case GET_TYPE -> newActionResponse(getTypeValue()).asPromise();
            case VERSION_INFO -> getVersionInfo(context, request);
            case GET_LATEST_TYPE -> getLatestType(context, request);
            default -> RequestHandler.super.handleAction(context, request);
            };
        }
        return router.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        if (context.asContext(UriRouterContext.class).getRemainingUri().isEmpty()) {
            return newResourceException(BAD_REQUEST, "Missing node id").asPromise();
        }
        return router.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return router.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return router.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return router.handleQuery(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return router.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return router.handleUpdate(context, request);
    }
}
