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
 * Copyright 2026 Ping Identity Corporation.
 */
/*
 * Copyright 2026 Ping Identity Corp. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity Corporation
 * software or services. Ping Identity Corporation only offers such software or
 * services to legal entities who have entered into a binding license agreement
 * with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.node.api;

/**
 * The different capabilities of a node in the context of a page node. This is used to determine how the node should be
 * treated by the UI and page node.
 */
public enum PageNodeCompatibility {

    /**
     * The node is supported for use in the page node.
     */
    SUPPORTED,
    /**
     * The node is neither supported nor unsupported for use in the page node, and is the default value.
     */
    UNKNOWN,
    /**
     * The node is supported for use in the page node, but is treated independently of other nodes.
     */
    STANDALONE,
    /**
     * The node is not supported for use in the page node.
     */
    UNSUPPORTED;
}
