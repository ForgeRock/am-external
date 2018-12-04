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
import { get } from "lodash";
import { Grid } from "react-bootstrap";
import { t } from "i18next";
import React, { Component, Fragment } from "react";
import PropTypes from "prop-types";

import { addOrUpdate as addOrUpdateInstance }
    from "store/modules/remote/config/global/secretStores/singletons/instances/list";
import { addOrUpdate as addOrUpdateCreatables }
    from "store/modules/remote/config/global/secretStores/singletons/types/creatables";
import {
    getSingleton as getInstance,
    getCreatableTypesByType as getCreatableTypes
} from "org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresService";
import BackLink from "components/BackLink";
import connectWithStore from "components/redux/connectWithStore";
import EditGlobalSingletonSecretStore
    from "org/forgerock/openam/ui/admin/views/configuration/secretStores/edit/EditGlobalSingletonSecretStore";
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
        const [type] = this.props.router.params;
        try {
            const instance = await getInstance(type);
            const creatables = getCreatableTypes(type);
            this.props.addOrUpdateInstance(instance);
            this.props.addOrUpdateCreatables(creatables.result, type);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    render () {
        const mappingsTitle = get(this.props.creatables, "[0].name");
        const link = `#${Router.getLink(Router.configuration.routes.listGlobalSecretStores)}`;

        return (
            <Fragment>
                <BackLink link={ link } title={ t("console.secretStores.list.title") } />
                <Grid>
                    <EditGlobalSingletonSecretStore
                        instance={ this.props.instance }
                        isFetching={ this.state.isFetching }
                        mappingsTitle={ mappingsTitle }
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
        const [type] = props.router.params;
        return {
            creatables: state.remote.config.global.secretStores.singletons.types.creatables[type],
            instance: state.remote.config.global.secretStores.singletons.instances.list
                .filter((instance) => instance._type._id === type)[0]
        };
    },
    (dispatch) => ({
        addOrUpdateInstance: bindActionCreators(addOrUpdateInstance, dispatch),
        addOrUpdateCreatables: bindActionCreators(addOrUpdateCreatables, dispatch)
    })
);

export default withRouter(EditGlobalSecretStoreContainer);
