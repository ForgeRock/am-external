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
 * Copyright 2019-2021 ForgeRock AS.
 */
package org.forgerock.openam.federation.rest;

import static org.forgerock.openam.audit.AuditConstants.Component.CONFIG;

import java.lang.reflect.InvocationTargetException;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.federation.rest.hosted.HostedEntitiesCollectionProvider;
import org.forgerock.openam.federation.rest.remote.RemoteEntitiesCollectionProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.CrestPrivilegeAuthzModule;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the CREST routes necessary to expose the SAML2 configurations to REST clients.
 */
public class Saml2RestRouteProvider implements RestRouteProvider {

    private static final Logger logger = LoggerFactory.getLogger(Saml2RestRouteProvider.class);

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter.route("realm-config/saml2")
                .auditAs(CONFIG)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .through(Saml2ErrorHandler.class)
                .toAnnotatedSingleton(Saml2RequestHandler.class);
        realmRouter.route("realm-config/saml2/hosted")
                .auditAs(CONFIG)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .through(Saml2ErrorHandler.class)
                .toAnnotatedCollection(HostedEntitiesCollectionProvider.class);
        realmRouter.route("realm-config/saml2/remote")
                .auditAs(CONFIG)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .through(Saml2ErrorHandler.class)
                .toAnnotatedCollection(RemoteEntitiesCollectionProvider.class);
    }

    private static class Saml2ErrorHandler implements Filter {
        @Override
        public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
                RequestHandler next) {
            return next.handleAction(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
                RequestHandler next) {
            return next.handleCreate(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
                RequestHandler next) {
            return next.handleDelete(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
                RequestHandler next) {
            return next.handlePatch(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
                QueryResourceHandler handler, RequestHandler next) {
            return next.handleQuery(context, request, handler)
                       .thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
                RequestHandler next) {
            return next.handleRead(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
                RequestHandler next) {
            return next.handleUpdate(context, request).thenCatchRuntimeExceptionAsync(this::promiseFromException);
        }

        private <V> Promise<V, ResourceException> promiseFromException(RuntimeException ex) {
            if (ex.getCause() instanceof InvocationTargetException) {
                Throwable target = ((InvocationTargetException) ex.getCause()).getTargetException();
                if (target instanceof IllegalArgumentException) {
                    return new BadRequestException(target).asPromise();
                }
            }
            logger.error("Runtime exception encountered at SAML2 REST endpoint.", ex);
            return new InternalServerErrorException(ex).asPromise();
        }
    }
}
