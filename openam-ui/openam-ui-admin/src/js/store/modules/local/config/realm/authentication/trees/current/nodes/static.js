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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { t } from "i18next";

export const FAILURE_NODE_ID = "e301438c-0bd0-429c-ab0c-66126501069a";
export const FAILURE_NODE_TYPE = "failureNodeType";
export const INNER_TREE_NODE_TYPE = "InnerTreeEvaluatorNode";
export const PAGE_NODE_TYPE = "PageNode";
export const START_NODE_ID = "startNode";
export const START_NODE_TYPE = "startNodeType";
export const SUCCESS_NODE_ID = "70e691a5-1e33-4ac3-a356-e7b6d60d92e0";
export const SUCCESS_NODE_TYPE = "successNodeType";

export function isStaticNodeType (type) {
    return type === FAILURE_NODE_TYPE || type === SUCCESS_NODE_TYPE || type === START_NODE_TYPE;
}

export function failure (x = 0, y = 0) {
    return {
        [FAILURE_NODE_ID]: {
            displayName: t("console.authentication.trees.edit.nodes.failure.title"),
            connections: {},
            _outcomes: [],
            nodeType: FAILURE_NODE_TYPE,
            x,
            y
        }
    };
}

export function start (entryNodeId, x = 0, y = 0) {
    return {
        [START_NODE_ID]: {
            displayName: t("console.authentication.trees.edit.nodes.start.title"),
            connections: {
                outcome: entryNodeId
            },
            _outcomes: [{
                id: "outcome",
                displayName: "Outcome"
            }],
            nodeType: START_NODE_TYPE,
            x,
            y
        }
    };
}

export function success (x = 0, y = 0) {
    return {
        [SUCCESS_NODE_ID]: {
            displayName: t("console.authentication.trees.edit.nodes.success.title"),
            connections: {},
            _outcomes: [],
            nodeType: SUCCESS_NODE_TYPE,
            x,
            y
        }
    };
}
