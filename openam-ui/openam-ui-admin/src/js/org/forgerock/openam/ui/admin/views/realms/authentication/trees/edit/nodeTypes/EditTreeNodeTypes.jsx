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
 * Copyright 2017-2026 Ping Identity Corporation.
 */
import { isEmpty, map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { useState } from "react";

import FilteredList from "./FilteredList";
import GroupedList from "./GroupedList";
import SearchFieldWithReset from "org/forgerock/openam/ui/admin/views/realms/common/SearchFieldWithReset";

export default function EditTreeNodeTypes ({ groupedNodeTypes, nodeTypes }) {
    const [filter, setFilter] = useState("");

    function handleFilterChange (event) {
        setFilter(event.target.value);
    }

    function handleFilterClear () {
        setFilter("");
    }

    const content = isEmpty(filter)
        ? map(groupedNodeTypes, (group, groupKey) => (
            <GroupedList
                defaultIsOpen={ groupKey === "basicAuthentication" }
                group={ group }
                groupKey={ groupKey }
                key={ groupKey }
            />
        ))
        : (
            <FilteredList
                filter={ filter }
                nodeTypes={ nodeTypes }
            />
        );

    return (
        <div className="authtree-content-side authtree-content-left">
            <h4 className="authtree-content-side-title">
                { t("console.authentication.trees.edit.nodes.nodeTypes.title") }
            </h4>
            <SearchFieldWithReset
                isResetEnabled={ !!filter }
                label={ t("console.authentication.trees.edit.nodes.nodeTypes.label") }
                onBlur={ handleFilterChange }
                onChange={ handleFilterChange }
                onClear={ handleFilterClear }
                value={ filter }
            />
            <div>
                { content }
            </div>
        </div>
    );
}

EditTreeNodeTypes.propTypes = {
    groupedNodeTypes: PropTypes.objectOf(PropTypes.array),
    nodeTypes: PropTypes.objectOf(PropTypes.array)
};
