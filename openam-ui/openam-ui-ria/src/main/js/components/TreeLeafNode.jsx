/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

import EmphasizedText from "./EmphasizedText";

const TreeLeafNode = ({ filter, highlighted, node, onSelect }) => {
    const onLeafSelect = () => { onSelect(node.path); };

    return (
        <li className={ `${highlighted ? "active" : ""}` }>
            { /* eslint-disable jsx-a11y/anchor-is-valid */ }
            <a
                className="am-tree-node-leaf"
                onClick={ onLeafSelect }
                onKeyPress={ onLeafSelect }
                role="button"
                tabIndex="0"
            >
                <EmphasizedText match={ filter }>{ node.id }</EmphasizedText>
            </a>
            { /* eslint-enable */ }
        </li>
    );
};

TreeLeafNode.propTypes = {
    filter: PropTypes.string,
    highlighted: PropTypes.bool.isRequired,
    node: PropTypes.objectOf({
        id: PropTypes.string.isRequired,
        path: PropTypes.string
    }).isRequired,
    onSelect: PropTypes.func.isRequired
};

export default TreeLeafNode;
