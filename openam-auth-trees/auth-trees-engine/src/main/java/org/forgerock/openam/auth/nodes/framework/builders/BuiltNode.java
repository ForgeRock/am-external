/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

/**
 * Describes a Node that has been built. It has an instance and build config is already in place.
 */
public class BuiltNode {

    private String id;

    /**
     * Constructs a BuiltNode.
     *
     * @param id the ID of the node.
     */
    public BuiltNode(String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the Node.
     * @return the ID.
     */
    String getId() {
        return id;
    }
}
