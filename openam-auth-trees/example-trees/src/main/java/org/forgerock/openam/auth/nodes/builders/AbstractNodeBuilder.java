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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import static org.forgerock.am.trees.api.TreeBuilder.DISPLAY_NAME;
import static org.forgerock.am.trees.api.TreeBuilder.NODE_TYPE;
import static org.forgerock.am.trees.api.TreeBuilder.VERSION;
import static org.forgerock.openam.core.realms.Realms.root;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.am.trees.api.NodeBuilder;
import org.forgerock.am.trees.api.TreeBuilder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * A base class that provides default functionality for NodeBuilder implementations.
 */
public abstract class AbstractNodeBuilder implements NodeBuilder {

    private String id;
    private String displayName;
    private Class<? extends Node> clazz;

    AbstractNodeBuilder(String displayName, Class<? extends Node> clazz) {
        this.displayName = displayName;
        this.id = UUID.randomUUID().toString();
        this.clazz = clazz;
    }

    /**
     * Sets the id of the node.
     *
     * @param id the UUID.
     * @throws IllegalArgumentException if the provided string is not a valid UUID
     * @return the builder.
     */
    public NodeBuilder uuid(String id) {
        this.id = UUID.fromString(id).toString();
        return this;
    }

    /**
     * Sets the display name of the node.
     *
     * @param displayName the display name.
     * @return the builder.
     */
    public NodeBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Returns the display name for this node.
     *
     * @return the display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the ID of the node.
     *
     * @return the ID.
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Accepts a TreeBuilder and performs some building operations on it. Visitor pattern.
     *
     * @param treeBuilder the tree builder.
     */
    @Override
    public void accept(TreeBuilder treeBuilder) {
        Map<String, Set<String>> attrs = Map.of(
                NODE_TYPE, asSet(treeBuilder.getNodeServiceName(clazz)),
                DISPLAY_NAME, asSet(displayName),
                VERSION, asSet(treeBuilder.getNodeVersion(clazz).toString())
        );
        treeBuilder.addNodeConfig(this.id, attrs);
    }

    /**
     * Builds the node using it's config.
     *
     * @param serviceRegistry the service registry.
     * @throws SMSException if there is a SMSException.
     * @throws SSOException if there is a SSOException.
     */
    @Override
    public void build(AnnotatedServiceRegistry serviceRegistry) throws SMSException, SSOException {
        serviceRegistry.createRealmInstance(root(), this.getId(), this);
    }
}
