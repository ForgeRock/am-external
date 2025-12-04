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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

import { bindActionCreators } from "redux";
import { forEach, map, reduce } from "lodash";
import { t } from "i18next";
import { Alert } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, getCreatableTypes, getSingletonTypes, remove } from
    "org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresService";
import { remove as removeInstance, set as setInstances, withCompositeId } from
    "store/modules/remote/config/global/secretStores/instances/list";
import { set as setSingletonStoreTypes } from "store/modules/remote/config/global/secretStores/singletons/types/list";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import ListGlobalSecretStores from "./ListGlobalSecretStores";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";

class ListGlobalSecretStoresContainer extends Component {
    constructor (props) {
        super(props);

        this.state = { isFetching: true };
    }

    async componentDidMount () {
        try {
            const [typesResponse, singletonResponse] = await Promise.all([getCreatableTypes(), getSingletonTypes()]);
            const types = map(typesResponse.result, (item) => item._id);
            const storesResponses = await Promise.all(map(types, (type) => getAll(type)));
            const stores = reduce(storesResponses, (allStores, response) => allStores.concat(response.result), []);

            this.props.setInstances(stores);
            this.props.setSingletonStoreTypes(singletonResponse);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); //eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleDelete = (selectedItems) => {
        const removingAll = this.props.stores.length === selectedItems.length;

        let additionalWarning;
        if (removingAll) {
            additionalWarning = (
                <Alert bsStyle="warning">
                    <div
                        dangerouslySetInnerHTML={ { __html: t("console.secretStores.list.deleteAll") } } // eslint-disable-line react/no-danger
                    />
                </Alert>
            );
        }

        showDeleteDialog({
            children: additionalWarning,
            names: selectedItems.map((item) => item._id),
            objectName: "secretStore",
            onConfirm: async () => {
                try {
                    await remove(selectedItems);
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                    forEach(selectedItems, (item) => this.props.removeInstance(item._compositeId));
                } catch (error) {
                    Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
                }
            }
        });
    };

    handleEdit = (e, { _id, _type }) => {
        Router.routeTo(Router.configuration.routes.editGlobalSecretStores, {
            args: map([_type._id, _id], encodeURIComponent),
            trigger: true
        });
    };

    render () {
        return (
            <ListGlobalSecretStores
                isFetching={ this.state.isFetching }
                items={ this.props.stores }
                keyField="_compositeId"
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
                singletonStoreTypes={ this.props.singletonStoreTypes }
            />
        );
    }
}

ListGlobalSecretStoresContainer.propTypes = {
    removeInstance: PropTypes.func.isRequired,
    setInstances: PropTypes.func.isRequired,
    setSingletonStoreTypes: PropTypes.func.isRequired,
    singletonStoreTypes: PropTypes.arrayOf(PropTypes.object).isRequired,
    stores: PropTypes.arrayOf(PropTypes.object).isRequired
};

ListGlobalSecretStoresContainer = connectWithStore(ListGlobalSecretStoresContainer,
    (state) => ({
        singletonStoreTypes: state.remote.config.global.secretStores.singletons.types.list,
        stores: withCompositeId(state.remote.config.global.secretStores.instances.list)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setSingletonStoreTypes: bindActionCreators(setSingletonStoreTypes, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListGlobalSecretStoresContainer = withRouter(ListGlobalSecretStoresContainer);

export default ListGlobalSecretStoresContainer;
