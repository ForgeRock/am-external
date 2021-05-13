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
 * Copyright 2019-2020 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { find, isEmpty, map, mapValues, reduce, sortBy } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";

import { FAILURE_NODE_TYPE, SUCCESS_NODE_TYPE } from
    "store/modules/local/config/realm/authentication/trees/current/nodes/static";
import { getAllTypes } from "org/forgerock/openam/ui/admin/services/realm/authentication/NodeService";
import { set as setNodeTypes } from "store/modules/local/config/realm/authentication/trees/nodeTypes/list";
import { set as setGroupedNodeTypes } from "store/modules/local/config/realm/authentication/trees/nodeTypes/grouped";
import connectWithStore from "components/redux/connectWithStore";
import EditTreeNodeTypes from "./EditTreeNodeTypes";
import groupsData from "./groupsData";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const successNode = {
    name: t("console.authentication.trees.edit.nodes.success.title"),
    _id: SUCCESS_NODE_TYPE,
    tags: ["basic authentication"]
};
const failureNode = {
    name: t("console.authentication.trees.edit.nodes.failure.title"),
    _id: FAILURE_NODE_TYPE,
    tags: ["basic authentication"]
};

let EditTreeNodeTypesContainer = ({ router, setGroupedNodeTypes, setNodeTypes }) => {
    const [isFetching, setIsFetching] = useState(true);
    const realm = router.params[0];

    useEffect(() => {
        getAllTypes(realm).then((response) => {
            const nodeTypesWithIcons = map({ failureNode, successNode, ...response.result }, (nodeType) => {
                const matchingGroup = find(groupsData, (groupName) =>
                    nodeType.tags.join().toLowerCase().includes(groupName.tag.toLowerCase()));
                nodeType.icon = matchingGroup ? matchingGroup.icon : groupsData.uncategorized.icon;
                return nodeType;
            });

            const sortedNodeTypes = sortBy(nodeTypesWithIcons, (node) => node.name.toUpperCase());

            const nodeTypesWithinGroups = reduce(groupsData, (result, { tag }, key) => {
                const groupName = tag.toLowerCase();
                result[key] = reduce(sortedNodeTypes, (result, nodeType) => {
                    if (nodeType.tags.join().toLowerCase().includes(groupName)) {
                        result.push(nodeType);
                    }
                    return result;
                }, []);
                return result;
            }, {});

            const groupNames = map(groupsData, "tag");

            const uncategorizedNodes = sortedNodeTypes.filter((nodeType) => {
                if (isEmpty(nodeType.tags)) {
                    return true;
                }

                const hasMatchingGroupName = groupNames.some((tag) => nodeType.tags.includes(tag));
                return !hasMatchingGroupName;
            });

            const groupNodeTypes = mapValues({
                ...nodeTypesWithinGroups,
                uncategorized: [
                    ...nodeTypesWithinGroups["uncategorized"],
                    ...uncategorizedNodes
                ]
            }, (tagGroup) => tagGroup);

            setGroupedNodeTypes(groupNodeTypes);
            setNodeTypes(sortedNodeTypes);
            setIsFetching(false);
        }, (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
    }, [realm, setGroupedNodeTypes, setNodeTypes]);

    return isFetching
        ? <Loading />
        : <EditTreeNodeTypes />;
};

EditTreeNodeTypesContainer.propTypes = {
    router: withRouterPropType,
    setGroupedNodeTypes: PropTypes.func.isRequired,
    setNodeTypes: PropTypes.func.isRequired
};

EditTreeNodeTypesContainer = connectWithStore(EditTreeNodeTypesContainer,
    (state) => ({
        groupedNodeTypes: state.local.config.realm.authentication.trees.nodeTypes.grouped,
        nodeTypes: state.local.config.realm.authentication.trees.nodeTypes.list
    }),
    (dispatch) => ({
        setGroupedNodeTypes: bindActionCreators(setGroupedNodeTypes, dispatch),
        setNodeTypes: bindActionCreators(setNodeTypes, dispatch)
    })
);

EditTreeNodeTypesContainer = withRouter(EditTreeNodeTypesContainer);

export default EditTreeNodeTypesContainer;
