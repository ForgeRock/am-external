/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { map, sortBy } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import EditTreeNodeTypeItem from "./EditTreeNodeTypeItem";
import {
    FAILURE_NODE_TYPE,
    SUCCESS_NODE_TYPE
} from "store/modules/local/config/realm/authentication/trees/current/nodes/static";

const EditTreeNodeTypes = ({ nodeTypes }) => {
    const sortedNodeTypes = sortBy(nodeTypes, "name");
    const nodeTypeItems = map(sortedNodeTypes, ({ _id, name }) =>
        <EditTreeNodeTypeItem displayName={ name } key={ _id } nodeType={ _id } />
    );
    const localePath = "console.authentication.trees.edit.nodes";

    return (
        <div className="authtree-content-side authtree-content-left">
            <h4 className="authtree-content-side-title">{ t(`${localePath}.nodeTypes.title`) }</h4>
            <EditTreeNodeTypeItem
                displayName={ t(`${localePath}.success.title`) }
                key={ SUCCESS_NODE_TYPE }
                nodeType={ SUCCESS_NODE_TYPE }
            />
            <EditTreeNodeTypeItem
                displayName={ t(`${localePath}.failure.title`) }
                key={ FAILURE_NODE_TYPE }
                nodeType={ FAILURE_NODE_TYPE }
            />
            <hr />
            { nodeTypeItems }
        </div>
    );
};

EditTreeNodeTypes.propTypes = {
    nodeTypes: PropTypes.objectOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired
    })).isRequired
};

export default EditTreeNodeTypes;
