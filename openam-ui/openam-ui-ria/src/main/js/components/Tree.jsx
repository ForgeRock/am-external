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
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 */

import _ from "lodash";
import React, { PropTypes } from "react";
import TreeLeafNode from "./TreeLeafNode";
import TreeNode from "./TreeNode";

const Tree = ({ activePath, collapsed, data, filter, onNodeClick }) => {
    // We've disabled the rule here as we are not creating multiple components in this file
    const createNode = (node, activePath) => { // eslint-disable-line react/no-multi-comp
        const isNodeHighlighted = _.head(activePath) === node.id;

        if (node.children) {
            const children = node.children.map((node) => createNode(node, _.drop(activePath)));

            return (
                <TreeNode
                    collapsed={ collapsed }
                    filter={ filter }
                    highlighted={ isNodeHighlighted }
                    node={ node }
                    onClick={ onNodeClick }
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
                    onClick={ onNodeClick }
                />
            );
        }
    };

    return (
        <ol className="am-tree list-unstyled">
            { data.map((node) => createNode(node, activePath)) }
        </ol>
    );
};

Tree.propTypes = {
    activePath: React.PropTypes.arrayOf(PropTypes.string).isRequired,
    collapsed: PropTypes.bool,
    data: React.PropTypes.arrayOf(PropTypes.objectOf({
        id: PropTypes.string.isRequired,
        children: PropTypes.array,
        path: PropTypes.string
    })).isRequired,
    filter: PropTypes.string,
    onNodeClick: PropTypes.func.isRequired
};

export default Tree;
