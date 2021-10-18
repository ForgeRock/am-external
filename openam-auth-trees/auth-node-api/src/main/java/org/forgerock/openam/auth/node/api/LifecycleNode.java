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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

/**
 * A Lifecycle Node is a node which is required to have some understanding of the lifecycle of a tree.  Usually
 * nodes should have a limited understanding of context outside their of own responsibility.  However rarely a node may
 * need to be notified of a change in tree state.  The current example of this is that the Retry Node needs to know
 * when the tree has completed successfully so that it can reset it's retry count.
 */
public interface LifecycleNode {

    /**
     * Called as part of tree completion, either Success or Failure. Intended to clean up any data left over by a node.
     *
     * @param context the tree context.
     * @param success true if the tree execution has resulted in Success, false if the tree has resulted in Failure.
     * @throws NodeProcessException if there is a problem that means an exception should be thrown.
     */
    default void onTreeComplete(TreeContext context, boolean success) throws NodeProcessException {
        // do nothing
    }
}
