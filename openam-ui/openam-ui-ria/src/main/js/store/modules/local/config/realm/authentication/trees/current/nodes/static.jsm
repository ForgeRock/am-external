/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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

export function failure () {
    return {
        [FAILURE_NODE_ID]: {
            displayName: t("console.authentication.trees.edit.nodes.failure.title"),
            connections: {},
            _outcomes: [],
            nodeType: FAILURE_NODE_TYPE
        }
    };
}

export function start (entryNodeId) {
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
            nodeType: START_NODE_TYPE
        }
    };
}

export function success () {
    return {
        [SUCCESS_NODE_ID]: {
            displayName: t("console.authentication.trees.edit.nodes.success.title"),
            connections: {},
            _outcomes: [],
            nodeType: SUCCESS_NODE_TYPE
        }
    };
}
