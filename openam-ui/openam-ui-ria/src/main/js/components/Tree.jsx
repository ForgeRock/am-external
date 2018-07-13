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
 * Copyright 2016-2017 ForgeRock AS.
 */

import { includes, map } from "lodash";
import React, { PropTypes } from "react";
import TreeLeafNode from "./TreeLeafNode";
import TreeNode from "./TreeNode";

const Tree = ({ activePaths, collapsed, data, filter, onNodeSelect }) => {
    // We've disabled the rule here as we are not creating multiple components in this file
    const createNode = (node, activePaths) => { // eslint-disable-line react/no-multi-comp
        const isNodeHighlighted = includes(activePaths, node.objectPath);

        if (node.children) {
            const children = map(node.children, (node) => createNode(node, activePaths));

            return (
                <TreeNode
                    collapsed={ collapsed }
                    filter={ filter }
                    highlighted={ isNodeHighlighted }
                    node={ node }
                    onSelect={ onNodeSelect }
                >
                    { children }
                </TreeNode>
            );
        } else {
            return (
                <TreeLeafNode
                    filter={ filter }
                    highlighted={ isNodeHighlighted }
                    node={ node }
                    onSelect={ onNodeSelect }
                />
            );
        }
    };

    return (
        <ol className="am-tree list-unstyled">
            { data.map((node) => createNode(node, activePaths)) }
        </ol>
    );
};

Tree.propTypes = {
    activePaths: React.PropTypes.arrayOf(PropTypes.string).isRequired,
    collapsed: PropTypes.bool,
    data: React.PropTypes.arrayOf(PropTypes.objectOf({
        id: PropTypes.string.isRequired,
        children: PropTypes.array,
        objectPath: PropTypes.string.isRequired,
        path: PropTypes.string
    })).isRequired,
    filter: PropTypes.string,
    onNodeSelect: PropTypes.func.isRequired
};

export default Tree;
