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
 * Copyright 2017-2018 ForgeRock AS.
 */
import { filter, map, sortBy } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import EditTreeNodeTypeItem from "./EditTreeNodeTypeItem";
import EditTreeNodeTypeFilter from "./EditTreeNodeTypeFilter";
import {
    FAILURE_NODE_TYPE,
    SUCCESS_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";

class EditTreeNodeTypes extends Component {
    constructor (props) {
        super(props);
        this.state = { filter: "" };
        this.handleFilterChange = this.handleFilterChange.bind(this);
        this.handleFilterClear = this.handleFilterClear.bind(this);
    }

    handleFilterChange (event) {
        this.setState({
            filter: event.target.value
        });
    }

    handleFilterClear () {
        this.setState({
            filter: ""
        });
    }

    render () {
        const filterMatches = (name) => name.toUpperCase().includes(this.state.filter.toUpperCase());
        const filteredItems = [];
        const successNodeName = t("console.authentication.trees.edit.nodes.success.title");
        if (filterMatches(successNodeName)) {
            filteredItems.push(
                <EditTreeNodeTypeItem
                    displayName={ successNodeName }
                    filter={ this.state.filter }
                    key={ SUCCESS_NODE_TYPE }
                    nodeType={ SUCCESS_NODE_TYPE }
                />
            );
        }
        const failureNodeName = t("console.authentication.trees.edit.nodes.failure.title");
        if (filterMatches(failureNodeName)) {
            filteredItems.push(
                <EditTreeNodeTypeItem
                    displayName={ failureNodeName }
                    filter={ this.state.filter }
                    key={ FAILURE_NODE_TYPE }
                    nodeType={ FAILURE_NODE_TYPE }
                />
            );
        }
        const filteredNodeTypes = filter(this.props.nodeTypes, (nodeType) => filterMatches(nodeType.name));
        if (filteredItems.length && filteredNodeTypes.length) {
            filteredItems.push(<hr />);
        }

        const filteredNodeTypeItems = map(
            sortBy(filteredNodeTypes, (node) => node.name.toUpperCase()), ({ _id, name }) => (
                <EditTreeNodeTypeItem
                    displayName={ name }
                    filter={ this.state.filter }
                    key={ _id }
                    nodeType={ _id }
                />
            ));

        filteredItems.push(...filteredNodeTypeItems);

        const content = filteredItems.length
            ? filteredItems
            : <div className="text-muted text-center">{ t("console.common.noResults") }</div>;

        return (
            <div className="authtree-content-side authtree-content-left">
                <h4 className="authtree-content-side-title">
                    { t("console.authentication.trees.edit.nodes.nodeTypes.title") }
                </h4>
                <EditTreeNodeTypeFilter
                    filter={ this.state.filter }
                    onChange={ this.handleFilterChange }
                    onClear={ this.handleFilterClear }
                />
                { content }
            </div>
        );
    }
}

EditTreeNodeTypes.propTypes = {
    nodeTypes: PropTypes.objectOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired
    })).isRequired
};

export default EditTreeNodeTypes;
