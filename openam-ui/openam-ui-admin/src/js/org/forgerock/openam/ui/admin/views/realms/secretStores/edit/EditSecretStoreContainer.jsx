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
import { get } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { addOrUpdate as addOrUpdateCreatables } from
    "store/modules/remote/config/realm/secretStores/types/creatables";
import { addOrUpdate as addOrUpdateInstance } from
    "store/modules/remote/config/realm/secretStores/instances/list";
import { get as getInstance, getCreatableTypesByType as getCreatableTypes, remove } from
    "org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService";
import connectWithStore from "components/redux/connectWithStore";
import EditSecretStore from "org/forgerock/openam/ui/admin/views/common/secretStores/edit/EditSecretStore";
import MappingsContainer from "./MappingsContainer";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SettingsContainer from "./SettingsContainer";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditSecretStoreContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };
    }

    async componentDidMount () {
        const [realm, type, id] = this.props.router.params;
        try {
            const [instance, creatables] = await Promise.all([
                getInstance(realm, type, id),
                getCreatableTypes(realm, type)
            ]);
            this.props.addOrUpdateCreatables(creatables.result, type);
            this.props.addOrUpdateInstance(instance);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleDelete = () => {
        const [realm] = this.props.router.params;
        showConfirmationBeforeAction({
            message: t("console.common.confirmDeleteItem")
        }, async () => {
            try {
                await remove(realm, [this.props.instance]);
                Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                Router.routeTo(Router.configuration.routes.realmsSecretStores, {
                    args: [encodeURIComponent(realm)], trigger: true
                });
            } catch (error) {
                Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
            }
        });
    };

    render () {
        const mappingsTitle = get(this.props.creatables, "[0].name");
        const mappingsChild = mappingsTitle ? <MappingsContainer /> : null;

        return (
            <EditSecretStore
                instance={ this.props.instance }
                isFetching={ this.state.isFetching }
                mappingsChild={ mappingsChild }
                mappingsTitle={ mappingsTitle }
                onDelete={ this.handleDelete }
                settingsChild={ <SettingsContainer /> }
            />
        );
    }
}

EditSecretStoreContainer.propTypes = {
    addOrUpdateCreatables: PropTypes.func.isRequired,
    addOrUpdateInstance: PropTypes.func.isRequired,
    creatables: PropTypes.arrayOf(PropTypes.object),
    instance: PropTypes.objectOf(PropTypes.any),
    router: withRouterPropType
};

EditSecretStoreContainer = connectWithStore(EditSecretStoreContainer,
    (state, props) => {
        const [, type, id] = props.router.params;
        return {
            creatables: state.remote.config.realm.secretStores.types.creatables[type],
            instance: state.remote.config.realm.secretStores.instances.list
                .filter((instance) => instance._id === id && instance._type._id === type)[0]
        };
    },
    (dispatch) => ({
        addOrUpdateInstance: bindActionCreators(addOrUpdateInstance, dispatch),
        addOrUpdateCreatables: bindActionCreators(addOrUpdateCreatables, dispatch)
    })
);

export default withRouter(EditSecretStoreContainer);
