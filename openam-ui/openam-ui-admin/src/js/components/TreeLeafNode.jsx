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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

import EmphasizedText from "./EmphasizedText";

class TreeLeafNode extends Component {
    handleLeafSelect = () => {
        this.props.onSelect(this.props.node.path);
    };

    render () {
        return (
            <li className={ `${this.props.highlighted ? "active" : ""}` }>
                { /* eslint-disable jsx-a11y/anchor-is-valid */ }
                <a
                    className="am-tree-node-leaf"
                    onClick={ this.handleLeafSelect }
                    onKeyPress={ this.handleLeafSelect }
                    role="button"
                    tabIndex="0"
                >
                    <EmphasizedText match={ this.props.filter }>{ this.props.node.id }</EmphasizedText>
                </a>
                { /* eslint-enable */ }
            </li>
        );
    }
}

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
