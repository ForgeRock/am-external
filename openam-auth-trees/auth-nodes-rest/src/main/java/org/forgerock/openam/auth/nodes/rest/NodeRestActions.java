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

/**
 * A class containing constants for the REST actions that are used by the Node REST API.
 */
public final class NodeRestActions {

    private NodeRestActions() {
        // Prevent instantiation
    }

    /**
     * The action ID to return a list of all the outcomes for the nodes.
     */
    public static final String LIST_OUTCOMES = "listOutcomes";
    /**
     * The action ID to return a list of all the latest types of each node.
     */
    public static final String LIST_LATEST_TYPES = "listLatestTypes";
    /**
     * The action ID to return the latest type of node.
     */
    public static final String GET_LATEST_TYPE = "getLatestType";
    /**
     * The action ID to return information about versions of a node.
     */
    public static final String VERSION_INFO = "versionInfo";
    /**
     * Get the upgraded configuration for a node.
     */
    public static final String GET_UPGRADED_CONFIG = "getUpgradedConfig";
    /**
     * The action ID to return the configuration provider script for a node.
     */
    public static final String CONFIG_PROVIDER_SCRIPT = "configProviderScript";
    /**
     * The action ID to return all available nodes of a realm, including their schema, template and outcomes.
     */
    public static final String LIST_LATEST_NODE_DEFINITIONS = "listLatestNodeDefinitions";
}
