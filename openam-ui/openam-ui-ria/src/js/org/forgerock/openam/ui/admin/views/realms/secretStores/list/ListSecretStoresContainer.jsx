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
 * Copyright 2018 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { forEach, map, reduce } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAllByType, getCreatableTypes, remove }
    from "org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService";
import { remove as removeInstance, set as setInstances, withCompositeId }
    from "store/modules/remote/config/realm/secretStores/instances/list";
import connectWithStore from "components/redux/connectWithStore";
import ListSecretStores from "./ListSecretStores";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListSecretStoresContainer extends Component {
    constructor (props) {
        super(props);

        this.state = { isFetching: true };

        this.handleDelete = this.handleDelete.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
    }

    async componentDidMount () {
        const realm = this.props.router.params[0];

        try {
            const { result } = await getCreatableTypes(realm);
            const creatableTypes = map(result, (item) => item._id);
            const getResponses = await Promise.all(map(creatableTypes, (type) => getAllByType(realm, type)));
            const stores = reduce(getResponses, (allStores, { result }) => allStores.concat(result), []);

            this.props.setInstances(stores);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleDelete (selectedItems) {
        const realm = this.props.router.params[0];
        const removingAll = this.props.stores.length === selectedItems.length;
        const confirmMessage = removingAll
            ? t("console.secretStores.confirmDeleteAll", { count: selectedItems.length })
            : t("console.secretStores.confirmDeleteSelected", { count: selectedItems.length });

        showConfirmationBeforeAction({
            message: confirmMessage
        }, async () => {
            try {
                await remove(realm, selectedItems);
                forEach(selectedItems, (item) => {
                    this.props.removeInstance(item._compositeId);
                });
                Messages.messages.displayMessageFromConfig("changesSaved");
            } catch (error) {
                Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
            }
        });
    }

    handleEdit (item) {
        const realm = this.props.router.params[0];
        const id = item._id;
        const type = item._type._id;

        Router.routeTo(Router.configuration.routes.realmsSecretStoresEdit, {
            args: map([realm, type, id], encodeURIComponent),
            trigger: true
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsSecretStoresNew, [encodeURIComponent(realm)]);

        return (
            <ListSecretStores
                isFetching={ this.state.isFetching }
                items={ this.props.stores }
                keyField="_compositeId"
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
            />
        );
    }
}

ListSecretStoresContainer.propTypes = {
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired,
    stores: PropTypes.arrayOf(PropTypes.object).isRequired
};

ListSecretStoresContainer = connectWithStore(ListSecretStoresContainer,
    (state) => ({
        stores: withCompositeId(state.remote.config.realm.secretStores.instances.list)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListSecretStoresContainer = withRouter(ListSecretStoresContainer);

export default ListSecretStoresContainer;
