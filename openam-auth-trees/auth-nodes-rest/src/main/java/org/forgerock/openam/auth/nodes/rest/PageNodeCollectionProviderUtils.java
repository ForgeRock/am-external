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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Requests.newDeleteRequest;
import static org.forgerock.json.resource.ResourcePath.resourcePath;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.auth.nodes.framework.PageNode;
import org.forgerock.services.context.Context;

/**
 * Utility class for handling the conversion of PageNode configuration to and from JSON.
 */
public class PageNodeCollectionProviderUtils {

    private static final String NODES_KEY = "nodes";
    private static final String ID = "_id";
    private static final String NODE_TYPE = "nodeType";
    private static final ResourcePath NODES_ENDPOINT =
            resourcePath("realm-config/authentication/authenticationtrees/nodes");
    private final ConnectionFactory connectionFactory;

    @Inject
    PageNodeCollectionProviderUtils(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Converts a JSON representation of a PageNode.
     * @param fromSms The JSON representation of the PageNode.
     * @return A different JSON representation of the PageNode.
     */
    protected JsonValue getJsonValue(JsonValue fromSms) {
        JsonValue nodes = json(array());
        for (String configString : fromSms.get(NODES_KEY).asList(String.class)) {
            nodes.add(PageNode.ChildNodeConfig.create(configString).asJson().getObject());
        }
        return fromSms.copy().put(NODES_KEY, nodes.getObject());
    }

    /**
     * Deletes the nodes contained within the page node.
     *
     * @param context request context.
     * @param response the response containing the nodes to be deleted.
     * @return the response after deletion.
     * @throws ResourceException if an error occurs during deletion.
     */
    protected ResourceResponse deleteNodes(Context context, ResourceResponse response) throws ResourceException {
        for (JsonValue node : response.getContent().get(NODES_KEY)) {
            final String nodeId = node.get(ID).asString();
            final String nodeType = node.get(NODE_TYPE).asString();
            connectionFactory.getConnection()
                    .delete(context, newDeleteRequest(NODES_ENDPOINT.child(nodeType)
                                                              .child(nodeId)));
        }
        return response;
    }

    /**
     * Converts the content of a JSON representation of a PageNode to a list of strings.
     * @param content The JSON representation of the PageNode.
     * @return A list of strings representing the nodes.
     */
    protected List<String> getAttributesFromContent(JsonValue content) {
        List<String> nodes = new ArrayList<>();
        for (JsonValue node : content.get(NODES_KEY)) {
            nodes.add(PageNode.ChildNodeConfig.create(node).toString());
        }
        return nodes;
    }
}
