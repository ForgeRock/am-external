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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.rest;

import static org.forgerock.openam.rest.RealmContext.getRealm;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Provides a filter for requests attempting to access the auth tree nodes endpoint.
 */
public class AuthTreeNodesFilter implements Filter {

    private final AuthTreeService authTreeService;

    @Inject
    AuthTreeNodesFilter(AuthTreeService authTreeService) {
        this.authTreeService = authTreeService;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest actionRequest,
                                                                   RequestHandler requestHandler) {
        return requestHandler.handleAction(context, actionRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest createRequest,
                                                                     RequestHandler requestHandler) {
        return requestHandler.handleCreate(context, createRequest);
    }

    /**
     * Do not allow deletion if the node exists within any tree in the system. Trees must have their
     * node references deleted prior to the deletion of the node itself.
     *
     * This enforces the pattern used in the XUI.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest deleteRequest,
                                                                     RequestHandler requestHandler) {
        Realm realm = getRealm(context);
        UUID uuid;
        try {
            uuid = UUID.fromString(deleteRequest.getResourcePath());
        } catch (IllegalArgumentException e) {
            return new BadRequestException("Resource to delete must be a valid UUID").asPromise();
        }
        Optional<AuthTree> containingTree = authTreeService.getAllEnabledTrees(realm).stream()
                .filter(tree -> tree.containsNode(uuid))
                .findAny();

        if (containingTree.isPresent()) {
            return new BadRequestException("Cannot delete a node that is connected in a tree.").asPromise();
        }

        return requestHandler.handleDelete(context, deleteRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest patchRequest,
                                                                    RequestHandler requestHandler) {
        return requestHandler.handlePatch(context, patchRequest);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest queryRequest,
                                                                 QueryResourceHandler queryResourceHandler,
                                                                 RequestHandler requestHandler) {
        return requestHandler.handleQuery(context, queryRequest, queryResourceHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest readRequest,
                                                                   RequestHandler requestHandler) {
        return requestHandler.handleRead(context, readRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest updateRequest,
                                                                     RequestHandler requestHandler) {
        return requestHandler.handleUpdate(context, updateRequest);
    }
}
