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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { get } from "lodash";
import { Grid } from "react-bootstrap";
import { t } from "i18next";
import React, { Component, Fragment } from "react";
import PropTypes from "prop-types";

import { addOrUpdate as addOrUpdateInstance } from "store/modules/remote/config/global/secretStores/instances/list";
import { addOrUpdate as addOrUpdateCreatables }
    from "store/modules/remote/config/global/secretStores/types/creatables";
import { get as getInstance, getCreatableTypesByType as getCreatableTypes, remove }
    from "org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresService";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import BackLink from "components/BackLink";
import connectWithStore from "components/redux/connectWithStore";
import EditSecretStore from "org/forgerock/openam/ui/admin/views/common/secretStores/edit/EditSecretStore";
import MappingsContainer from "./MappingsContainer";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SettingsContainer from "./SettingsContainer";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditGlobalSecretStoreContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };
    }

    async componentDidMount () {
        const [type, id] = this.props.router.params;
        try {
            const [instance, creatables] = await Promise.all([
                getInstance(type, id),
                getCreatableTypes(type)
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
        showDeleteDialog({
            names: [this.props.instance._id],
            objectName: "secretStore",
            onConfirm: async () => {
                try {
                    await remove([this.props.instance]);
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                    Router.routeTo(Router.configuration.routes.listGlobalSecretStores, { args: [], trigger: true });
                } catch (error) {
                    Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
                }
            }
        });
    };

    render () {
        const mappingsTitle = get(this.props.creatables, "[0].name");
        const mappingsChild = mappingsTitle ? <MappingsContainer /> : null;
        const link = `#${Router.getLink(Router.configuration.routes.listGlobalSecretStores)}`;

        return (
            <Fragment>
                <BackLink link={ link } title={ t("console.secretStores.list.title") } />
                <Grid>
                    <EditSecretStore
                        instance={ this.props.instance }
                        isFetching={ this.state.isFetching }
                        mappingsChild={ mappingsChild }
                        mappingsTitle={ mappingsTitle }
                        onDelete={ this.handleDelete }
                        settingsChild={ <SettingsContainer /> }
                    />
                </Grid>
            </Fragment>
        );
    }
}

EditGlobalSecretStoreContainer.propTypes = {
    addOrUpdateCreatables: PropTypes.func.isRequired,
    addOrUpdateInstance: PropTypes.func.isRequired,
    creatables: PropTypes.arrayOf(PropTypes.object),
    instance: PropTypes.objectOf(PropTypes.any),
    router: withRouterPropType
};

EditGlobalSecretStoreContainer = connectWithStore(EditGlobalSecretStoreContainer,
    (state, props) => {
        const [type, id] = props.router.params;
        return {
            creatables: state.remote.config.global.secretStores.types.creatables[type],
            instance: state.remote.config.global.secretStores.instances.list
                .filter((instance) => instance._id === id && instance._type._id === type)[0]
        };
    },
    (dispatch) => ({
        addOrUpdateInstance: bindActionCreators(addOrUpdateInstance, dispatch),
        addOrUpdateCreatables: bindActionCreators(addOrUpdateCreatables, dispatch)
    })
);

export default withRouter(EditGlobalSecretStoreContainer);
