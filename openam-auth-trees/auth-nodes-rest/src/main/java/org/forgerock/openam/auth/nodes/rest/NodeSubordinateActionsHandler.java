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

import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.Schema.schema;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.ResourcePath.empty;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.DESCRIPTION;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.OBJECT_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TITLE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TYPE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.AUTH_TREE_NODES_RESOURCE;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.GET_LATEST_TYPE;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_LATEST_TYPES;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_LATEST_NODE_DEFINITIONS;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_OUTCOMES;
import static org.forgerock.openam.rest.RestConstants.SCHEMA;
import static org.forgerock.openam.rest.RestConstants.TEMPLATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Action;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.SubordinateActionsHandler;
import org.forgerock.services.context.Context;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;

/**
 * Handles subordinate actions for nodes. A decorator for the GeneralActionsHandler.
 */
public class NodeSubordinateActionsHandler implements SubordinateActionsHandler {

    private final GeneralActionsHandler generalActionsHandler;
    private final List<Action> actions;

    /**
     * Constructor for NodeSubordinateActionsHandler.
     * @param generalActionsHandler The GeneralActionsHandler to delegate to for actions it handles.
     * @param stability The stability of the actions provided by this handler.
     */
    public NodeSubordinateActionsHandler(GeneralActionsHandler generalActionsHandler, Stability stability) {
        this.generalActionsHandler = generalActionsHandler;
        List<Action> temp = new ArrayList<>(generalActionsHandler.getActions());
        temp.add(action()
                         .name(LIST_LATEST_TYPES)
                         .response(schema().schema(json(object(
                                 field(TYPE, OBJECT_TYPE),
                                 field(TITLE, localString("listLatestTypes.response.title")),
                                 field(DESCRIPTION, localString("listLatestTypes.response.description"))
                         ))).build())
                         .description(localString("listLatestTypes.description"))
                         .stability(stability)
                         .build());
        temp.add(action()
                         .name(LIST_LATEST_NODE_DEFINITIONS)
                         .response(schema().schema(json(object(
                                 field(TYPE, OBJECT_TYPE),
                                 field(TITLE, localString("listLatestNodeDefinitions.response.title")),
                                 field(DESCRIPTION, localString("listLatestNodeDefinitions.response.description"))
                         ))).build())
                         .description(localString("listLatestNodeDefinitions.description"))
                         .stability(stability)
                         .build());
        actions = List.copyOf(temp);
    }

    private LocalizableString localString(String key) {
        return new LocalizableString(AUTH_TREE_NODES_RESOURCE + key, getClass());
    }

    @Override
    public boolean handles(String action) {
        return generalActionsHandler.handles(action) || LIST_LATEST_TYPES.equals(action)
                || LIST_LATEST_NODE_DEFINITIONS.equals(action);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String action, Map<String, RequestHandler> handlers,
            Context context, ActionRequest request, Set<String> hiddenFromUi) {
        if (generalActionsHandler.handles(action)) {
            return generalActionsHandler.handle(action, handlers, context, request, hiddenFromUi);
        } else if (LIST_LATEST_TYPES.equals(action)) {
            return getLatestTypes(handlers, context, request, hiddenFromUi);
        } else if (LIST_LATEST_NODE_DEFINITIONS.equals(action)) {
            return listLatestNodeDefinitions(handlers, context, request, hiddenFromUi);
        }
        return new NotSupportedException().asPromise();
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    private Promise<ActionResponse, ResourceException> getLatestTypes(Map<String, RequestHandler> handlers,
            Context context, ActionRequest request, Set<String> hiddenFromUi) {
        JsonValue result = json(array());
        for (Map.Entry<String, RequestHandler> subRoute : handlers.entrySet()) {
            if (!subRoute.getKey().isEmpty()) {
                var actionRequest = newActionRequest(empty(), GET_LATEST_TYPE)
                                            .setResourceVersion(request.getResourceVersion());
                try {
                    var response = subRoute.getValue()
                                                      .handleAction(context, actionRequest)
                                                      .getOrThrowIfInterrupted();
                    JsonValue jsonContent = response.getJsonContent();
                    if (!hiddenFromUi.contains(subRoute.getKey())) {
                        result.add(jsonContent.getObject());
                    }
                } catch (ResourceException e) {
                    return e.asPromise();
                }
            }
        }
        return newActionResponse(json(object(field("result", result.getObject())))).asPromise();
    }

    private Promise<ActionResponse, ResourceException> listLatestNodeDefinitions(Map<String, RequestHandler> handlers,
            Context context, ActionRequest request, Set<String> hiddenFromUi) {
        JsonValue result = json(object());
        for (Map.Entry<String, RequestHandler> subRoute : handlers.entrySet()) {
            if (!subRoute.getKey().isEmpty() && !hiddenFromUi.contains(subRoute.getKey())) {
                try {
                    ActionRequest getLatestTypeRequest = newActionRequest(empty(), GET_LATEST_TYPE)
                            .setResourceVersion(request.getResourceVersion());
                    getLatestTypeRequest.setPreferredLocales(request.getPreferredLocales());
                    ActionResponse getLatestTypeResponse = subRoute.getValue()
                            .handleAction(context, getLatestTypeRequest)
                            .getOrThrowIfInterrupted();
                    JsonValue latestTypeResponseJson = getLatestTypeResponse.getJsonContent();
                    JsonValue nodeData = latestTypeResponseJson.copy();

                    String version = latestTypeResponseJson.get("version").defaultTo("").asString();

                    for (String action : List.of(SCHEMA, TEMPLATE, LIST_OUTCOMES)) {
                        ActionRequest actionRequest = newActionRequest(resourcePath(version), action)
                                .setResourceVersion(request.getResourceVersion());
                        actionRequest.setPreferredLocales(request.getPreferredLocales());
                        ActionResponse response = subRoute.getValue()
                                .handleAction(context, actionRequest)
                                .getOrThrowIfInterrupted();
                        String attributeName = LIST_OUTCOMES.equals(action) ? "outcomes" : action;
                        nodeData.put(attributeName, response.getJsonContent());
                    }
                    result.put(subRoute.getKey(), nodeData.getObject());
                } catch (ResourceException e) {
                    return e.asPromise();
                }
            }
        }
        return newActionResponse(json(object(field("result", result.getObject())))).asPromise();
    }
}
