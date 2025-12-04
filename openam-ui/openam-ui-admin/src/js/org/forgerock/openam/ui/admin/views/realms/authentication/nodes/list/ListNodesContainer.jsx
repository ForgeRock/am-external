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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { forEach, map, values } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    getAll,
    remove,
    exportNodes
} from "org/forgerock/openam/ui/admin/services/global/authentication/NodeDesignerService";
import {
    remove as removeInstance,
    set as setInstance
} from "store/modules/remote/config/realm/authentication/nodes/instances";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import { t } from "i18next";
import ListNodes from "./ListNodes";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import { downloadFile } from "org/forgerock/openam/ui/admin/utils/downloadFile";

class ListNodesContainer extends Component {
    constructor () {
        super();

        this.state = { isFetching: true };
    }

    componentDidMount () {
        getAll().then((response) => {
            this.setState({ isFetching: false });
            this.props.setInstance(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleDelete = (items) => {
        const ids = items.map((item) => item._id);
        const displayNames = items.map((item) => item.displayName);

        showDeleteDialog({
            names: displayNames,
            objectName: "node_type",
            onConfirm: async () => {
                const response = await remove(ids);

                forEach(response, ({ _id }) => this.props.removeInstance(_id));
            }
        });
    };

    handleExport = (items) => {
        const nodeIds = items.map((node) => node._id);
        exportNodes(nodeIds)
            .then((data) => {
                const dataToExport = JSON.stringify(data, null, 2); // 2 is for spacing in json file
                const { meta } = data;
                downloadFile(
                    dataToExport,
                    "application/json",
                    `${nodeIds.join("-")}-nodes-${window.location.host}-${meta.exportDate}.json`
                );
            }).catch(() => {
                Messages.addMessage({
                    message: t("console.authentication.nodes.export.error"),
                    type: Messages.TYPE_DANGER
                });
            });
    };

    handleImport = () => {
        const realm = this.props.router.params[0];
        console.log(map([realm], encodeURIComponent));
        Router.routeTo(Router.configuration.routes.realmsAuthenticationNodesImport, {
            args: [encodeURIComponent(realm)],
            trigger: true
        });
    };

    handleEdit = (e, item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsAuthenticationNodesEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsAuthenticationNodesNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListNodes
                isFetching={ this.state.isFetching }
                items={ this.props.nodes }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onExport={ this.handleExport }
                onImport={ this.handleImport }
                onRowClick={ this.handleEdit }
            />
        );
    }
}

ListNodesContainer.propTypes = {
    nodes: PropTypes.arrayOf(PropTypes.object),
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setInstance: PropTypes.func.isRequired
};

ListNodesContainer = connectWithStore(ListNodesContainer,
    (state) => ({
        nodes: values(state.remote.config.realm.authentication.nodes.instances)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstance: bindActionCreators(setInstance, dispatch)
    })
);
ListNodesContainer = withRouter(ListNodesContainer);

export default ListNodesContainer;
