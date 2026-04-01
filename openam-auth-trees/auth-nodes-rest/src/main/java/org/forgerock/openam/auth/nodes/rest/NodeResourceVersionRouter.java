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

import static org.forgerock.api.enums.Stability.DEPRECATED;
import static org.forgerock.api.enums.Stability.EVOLVING;
import static org.forgerock.json.resource.Resources.newHandler;

import jakarta.inject.Inject;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
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
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptor;
import org.forgerock.openam.core.rest.sms.tree.SubordinateActionsDescriber;
import org.forgerock.openam.rest.api.deprecation.RestApiResourceVersionDeprecationFilter;
import org.forgerock.openam.rest.api.logging.ExponentialBackoffLoggingStrategy;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;

import com.google.inject.assistedinject.Assisted;

/**
 * A {@link RequestHandler} that routes requests to the appropriate version of the nodes SMS collection provider.
 */
public class NodeResourceVersionRouter implements RequestHandler, Describable<ApiDescription, Request> {

    private static final Version VERSION_1 = Version.version(1);
    private static final Version VERSION_2 = Version.version(2);
    private static final Version VERSION_3 = Version.version(3);
    private static final String AM_VERSION_8_1_0 = "8.1.0";

    private final Router router;

    @Inject
    NodeResourceVersionRouter(@Assisted SubordinateActionsInterceptor nodeProvider,
            @Assisted NodeVersionProvider versionsProvider, AuthTreeNodesFilter authTreeNodesFilter) {
        router = new Router();
        var loggingStrategy = ExponentialBackoffLoggingStrategy.builder().build();
        var legacyHandler = new SubordinateActionsDescriber(newHandler(nodeProvider),
                new GeneralActionsHandler(null, DEPRECATED));
        router.addRoute(VERSION_1, new FilterChain(legacyHandler, new RestApiResourceVersionDeprecationFilter(
                AM_VERSION_8_1_0, loggingStrategy, VERSION_1), authTreeNodesFilter));
        router.addRoute(VERSION_2, new FilterChain(legacyHandler, new RestApiResourceVersionDeprecationFilter(
                AM_VERSION_8_1_0, loggingStrategy, VERSION_2), authTreeNodesFilter));
        router.addRoute(VERSION_3, new SubordinateActionsDescriber(newHandler(versionsProvider),
                new GeneralActionsHandler(null, EVOLVING)));

    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return router.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
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

    @Override
    public ApiDescription api(final ApiProducer<ApiDescription> producer) {
        return router.api(producer);
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return router.handleApiRequest(context, request);
    }
}
