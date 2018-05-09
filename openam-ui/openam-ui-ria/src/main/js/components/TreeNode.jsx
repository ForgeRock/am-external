/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

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
