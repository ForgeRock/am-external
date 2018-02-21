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

import React, { Component, PropTypes } from "react";
import EmphasizedText from "./EmphasizedText";

class TreeNode extends Component {
    constructor (props) {
        super(props);
        this.handleOnNodeSelect = this.handleOnNodeSelect.bind(this);
        this.state = { collapsed: props.highlighted ? false : props.collapsed };
    }

    handleOnNodeSelect () {
        this.setState({ collapsed: !this.state.collapsed });

        if (this.props.node.path) {
            this.props.onSelect(this.props.node.path);
        }
    }

    render () {
        return (
            <li className={ `${this.props.highlighted ? "active" : ""}` }>
                { /* eslint-disable jsx-a11y/anchor-is-valid */ }
                <a
                    className="am-tree-node"
                    onClick={ this.handleOnNodeSelect }
                    onKeyPress={ this.handleOnNodeSelect }
                    role="button"
                    tabIndex="0"
                >
                    { /* eslint-enable */ }
                    <i className={ `fa ${this.state.collapsed ? "fa-plus-square-o" : "fa-minus-square-o"}` } />
                    <EmphasizedText match={ this.props.filter }>{ this.props.node.id }</EmphasizedText>
                </a>
                <div className={ this.state.collapsed ? "am-tree-node-children-hidden" : "am-tree-node-children" }>
                    { this.props.children }
                </div>
            </li>
        );
    }
}

TreeNode.propTypes = {
    children: PropTypes.element.isRequired,
    collapsed: PropTypes.bool,
    filter: PropTypes.string,
    highlighted: PropTypes.bool.isRequired,
    node: PropTypes.objectOf({
        id: PropTypes.string.isRequired,
        children: PropTypes.array,
        path: PropTypes.string
    }).isRequired,
    onSelect: PropTypes.func.isRequired
};

export default TreeNode;
