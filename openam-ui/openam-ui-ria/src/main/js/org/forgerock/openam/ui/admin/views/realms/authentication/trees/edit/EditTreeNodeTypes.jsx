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
 * Copyright 2017 ForgeRock AS.
 */
import { map, sortBy } from "lodash";
import { t } from "i18next";
import React, { PropTypes } from "react";

import EditTreeNodeTypeItem from "./EditTreeNodeTypeItem";
import { FAILURE_NODE_TYPE, SUCCESS_NODE_TYPE } from "store/modules/local/authentication/trees/current/nodes/static";

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
