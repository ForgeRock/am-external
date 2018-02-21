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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

/**
 * This class represents all the constants that can be used as keys for storing values in the tree's shared state.
 *
 * @supported.all.api
 */
public final class SharedStateConstants {

    /**
     * Private constructor.
     */
    private SharedStateConstants() {
    }

    /** The Realm. **/
    public static final String REALM = "realm";

    /** The current authentication level achieved so far processing the tree. **/
    public static final String AUTH_LEVEL = "authLevel";

    /** The desirable authentication level to achieve at successful authentication. **/
    public static final String TARGET_AUTH_LEVEL = "targetAuthLevel";

    /** The nodeId of the node being processed in the tree. **/
    public static final String CURRENT_NODE_ID = "currentNodeId";

    /** The username. **/
    public static final String USERNAME = "username";

    /** The user's password. **/
    public static final String PASSWORD = "password";
}
