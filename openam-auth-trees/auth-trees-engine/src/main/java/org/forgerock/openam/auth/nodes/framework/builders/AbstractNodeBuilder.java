/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import static org.forgerock.openam.auth.nodes.framework.builders.BuilderConstants.DISPLAY_NAME;
import static org.forgerock.openam.auth.nodes.framework.builders.BuilderConstants.NODE_TYPE;
import static org.forgerock.openam.core.realms.Realms.root;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.guava.common.collect.ImmutableMap;
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
     * @throws SMSException thrown if there is a SMS exception.
     * @throws SSOException if there is a SSO exception.
     */
    @Override
    public void accept(TreeBuilder treeBuilder) throws SMSException, SSOException {
        Map<String, Set<String>> attrs = ImmutableMap.of(
                NODE_TYPE, asSet(treeBuilder.getNodeServiceName(clazz)),
                DISPLAY_NAME, asSet(displayName));
        treeBuilder.addNodeConfig(this.id, attrs);
    }

    /**
     * Builds the node using it's config.
     *
     * @param serviceRegistry the service registry.
     * @return the built node.
     * @throws SMSException if there is a SMSException.
     * @throws SSOException if there is a SSOException.
     */
    @Override
    public BuiltNode build(AnnotatedServiceRegistry serviceRegistry) throws SMSException, SSOException {
        serviceRegistry.createRealmInstance(root(), this.getId(), this);
        return new BuiltNode(id);
    }
}
