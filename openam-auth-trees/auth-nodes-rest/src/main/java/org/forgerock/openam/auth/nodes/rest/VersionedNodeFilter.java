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

import static org.forgerock.openam.rest.RestConstants.GET_TYPE;
import static org.forgerock.openam.rest.RestConstants.ID;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A wrapper for an {@link SmsCollectionProvider} that adds version information to the response for the getType action.
 */
public class VersionedNodeFilter implements Filter {

    private final String version;

    VersionedNodeFilter(String version) {
        this.version = version;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest actionRequest,
            RequestHandler requestHandler) {
        return requestHandler.handleAction(context, actionRequest).then(result -> {
            if (GET_TYPE.equals(actionRequest.getAction())) {
                result.getJsonContent().put(ID, version);
            }
            return result;
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest createRequest,
            RequestHandler requestHandler) {
        return requestHandler.handleCreate(context, createRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest deleteRequest,
            RequestHandler requestHandler) {
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
