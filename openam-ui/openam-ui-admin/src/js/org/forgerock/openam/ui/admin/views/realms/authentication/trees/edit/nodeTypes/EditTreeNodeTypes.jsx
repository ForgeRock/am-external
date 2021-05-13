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
import { isEmpty, map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import connectWithStore from "components/redux/connectWithStore";
import FilteredList from "./FilteredList";
import GroupedList from "./GroupedList";
import SearchFieldWithReset from "org/forgerock/openam/ui/admin/views/realms/common/SearchFieldWithReset";

class EditTreeNodeTypes extends Component {
    state = { filter: "" };

    handleFilterChange = (event) => {
        this.setState({ filter: event.target.value });
    };

    handleFilterClear = () => {
        this.setState({ filter: "" });
    };

    render () {
        const content = isEmpty(this.state.filter)
            ? map(this.props.groupedNodeTypes,
                (group, groupKey) => (
                    <GroupedList
                        defaultIsOpen={ groupKey === "basicAuthentication" }
                        group={ group }
                        groupKey={ groupKey }
                        key={ groupKey }
                    />
                ))
            : <FilteredList filter={ this.state.filter } />;

        return (
            <div className="authtree-content-side authtree-content-left">
                <h4 className="authtree-content-side-title">
                    { t("console.authentication.trees.edit.nodes.nodeTypes.title") }
                </h4>
                <SearchFieldWithReset
                    isResetEnabled={ !!this.state.filter }
                    label={ t("console.authentication.trees.edit.nodes.nodeTypes.label") }
                    onBlur={ this.handleFilterChange }
                    onChange={ this.handleFilterChange }
                    onClear={ this.handleFilterClear }
                    value={ this.state.filter }
                />
                <div>
                    { content }
                </div>
            </div>
        );
    }
}

EditTreeNodeTypes.propTypes = {
    groupedNodeTypes: PropTypes.objectOf(PropTypes.array)
};

EditTreeNodeTypes = connectWithStore(EditTreeNodeTypes,
    (state) => ({
        groupedNodeTypes: state.local.config.realm.authentication.trees.nodeTypes.grouped
    })
);

export default EditTreeNodeTypes;
