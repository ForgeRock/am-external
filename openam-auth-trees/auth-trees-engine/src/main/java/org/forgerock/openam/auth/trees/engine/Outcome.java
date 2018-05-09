/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;

import java.util.UUID;

/**
 * The possible outcomes of trees and nodes.
 *
 * @since AM 5.5.0
 */
public enum Outcome {
    /** Tree results in a positive state. */
    TRUE,
    /** Tree results in a negative state. */
    FALSE,
    /** Tree requires input from the user. */
    NEED_INPUT;

    /**
     * Return the Outcome from the uuid.
     * @param nodeId the uuid of the node.
     * @return {@link Outcome#TRUE} if the NODE is success, otherwise {@link Outcome#FALSE}.
     */
    public static Outcome getFromNodeID(UUID nodeId) {
        return nodeId.equals(TREE_NODE_SUCCESS_ID) ? Outcome.TRUE : Outcome.FALSE;
    }
}
