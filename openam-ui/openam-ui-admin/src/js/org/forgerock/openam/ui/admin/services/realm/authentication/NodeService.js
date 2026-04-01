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
 * Copyright 2017-2026 Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/authentication/NodeService
 */
import { CRESTv2_1 } from "@forgerock/crest-js";

import middleware from "api/crest/middleware";
import spinner from "api/crest/spinner";
import url from "api/crest/url";

const BASE_URL_PATH = "/realm-config/authentication/authenticationtrees/nodes";

const resourceV3 = (realm, type = "", nodeVersion = null) => {
    const path = nodeVersion ? `${BASE_URL_PATH}/${type}/${nodeVersion}` : `${BASE_URL_PATH}/${type}`;
    return new CRESTv2_1(url(path, realm), {
        middleware: [middleware],
        resourceVersion: "3.0"
    });
};

/**
  * Creates or Updates a node
  * @param {string} realm the current realm
  * @param {object} body the nodes config
  * @param {string} type Id specifiying a specific node type
  * @param {string} id the nodes id
  * @param {string} nodeVersion the version of the node
  *
  * @returns {Promise}
  */
export const createOrUpdateNode = (realm, body, type, id, nodeVersion) =>
    spinner(resourceV3(realm, type, nodeVersion).update(id, body));

/**
  * Returns a specific nodes data template
  * @param {string} realm the current realm
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeId Id of the node to get the template of
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeProperties = (realm, type, nodeId, nodeVersion) => resourceV3(realm, type, nodeVersion).get(nodeId);

/**
  * Returns a node types base schema
  * @param {string} realm the current realm
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeTypeSchema = (realm, type, nodeVersion) => resourceV3(realm, type, nodeVersion).action("schema");

/**
  * Returns a node types dynamic schema
  * @param {string} realm the current realm
  * @param {object} body the nodes config
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeTypeDynamicSchema = (realm, body, type, nodeVersion) =>
    resourceV3(realm, type, nodeVersion).action("schema", { body });

/**
  * Returns a node types base data template
  * @param {string} realm the current realm
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeTypeTemplate = (realm, type, nodeVersion) =>
    resourceV3(realm, type, nodeVersion).action("template");

/**
  * Returns a node types dynamic template
  * @param {string} realm the current realm
  * @param {object} body the nodes config
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeTypeDynamicTemplate = (realm, body, type, nodeVersion) =>
    resourceV3(realm, type, nodeVersion).action("template", { body });

/**
  * Returns a list of outcomes for a node
  * @param {string} realm the current realm
  * @param {object} body the nodes config
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const listNodeOutcomes = (realm, body, type, nodeVersion) =>
    resourceV3(realm, type, nodeVersion).action("listOutcomes", { body });

/**
  * Deletes a node
  * @param {string} realm the current realm
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeId Id of the node to get the template of
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const deleteNode = (realm, type, nodeId, nodeVersion) => resourceV3(realm, type, nodeVersion).delete(nodeId);

/**
  * Returns a specific nodes type data
  * @param {string} realm the current realm
  * @param {string} type Id specifiying a specific node type
  * @param {string} nodeVersion The version of the node
  *
  * @returns {Promise}
  */
export const getNodeType = (realm, type, nodeVersion) => resourceV3(realm, type, nodeVersion).action("getType");

/**
  * Returns a list of available node types
  * @param {string} realm the current realm
  *
  * @returns {Promise}
  */
export const getAllTypes = (realm) => resourceV3(realm).action("getAllTypes");

/**
  * Returns a list of the latest versions for each node type
  * @param {string} realm the current realm
  *
  * @returns {Promise}
  */
export const listLatestTypes = (realm) => resourceV3(realm).action("listLatestTypes");

/**
  * Returns version information about the given node type
  * @param {string} realm the current realm
  * @param {string} nodeType Id specifiying a specific node type
  *
  * @returns {Promise}
  */
export const actionNodeGetVersionInfo = (realm, nodeType) => resourceV3(realm, nodeType).action("versionInfo");

/**
  * Updates a node to the given target version
  * @param {string} realm the current realm
  * @param {string} nodeType Id specifiying a specific node type
  * @param {string} currentVersion The current version of the node
  * @param {string} targetVersion The version you wish to upgrade the node to
  * @param {object} nodeData The current node configuration
  *
  * @returns {Promise}
  */
export const actionNodeGetUpgradedConfig = (realm, nodeType, currentVersion, targetVersion, nodeData) => {
    const path = `${BASE_URL_PATH}/${nodeType}/${currentVersion}`;
    const crest = new CRESTv2_1(url(path, realm), { middleware: [middleware], resourceVersion: "3.0" });

    return crest.action("getUpgradedConfig", { queryString: { targetVersion }, body: nodeData });
};
