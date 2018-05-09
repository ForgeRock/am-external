/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Represents a Node builder. Uses visitor pattern for building and configuration.
 */
public interface NodeBuilder {

    /**
     * Accept a TreeBuilder and perform operations on it.
     *
     * @param treeBuilder the tree builder.
     * @throws SMSException if there was an SMS error.
     * @throws SSOException if there was an SSO error.
     */
    void accept(TreeBuilder treeBuilder) throws SMSException, SSOException;

    /**
     * Return the ID of the node being built.
     *
     * @return the ID.
     */
    String getId();

    /**
     * Build the Node, adding it to service registry.
     *
     * @param serviceRegistry the service registry.
     * @return the built node.
     * @throws SMSException if there was an SMS error.
     * @throws SSOException if there was an SSO error.
     */
    BuiltNode build(AnnotatedServiceRegistry serviceRegistry) throws SMSException, SSOException;
}
