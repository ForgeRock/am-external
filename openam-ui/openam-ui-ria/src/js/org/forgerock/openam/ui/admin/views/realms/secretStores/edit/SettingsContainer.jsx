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
import { find } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { addOrUpdate as addOrUpdateInstance } from "store/modules/remote/config/realm/secretStores/instances/list";
import { addOrUpdate as addOrUpdateSchema } from "store/modules/remote/config/realm/secretStores/types/schema";
import { getSchema, update } from "org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import Settings from "org/forgerock/openam/ui/admin/views/common/secretStores/edit/Settings";

class SettingsContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };

        this.handleSave = this.handleSave.bind(this);
    }

    async componentDidMount () {
        const [realm, type] = this.props.router.params;
        try {
            const schema = await getSchema(realm, type);
            this.props.addOrUpdateSchema(schema, type);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    async handleSave (values) {
        const [realm, type, id] = this.props.router.params;
        try {
            const instance = await update(realm, type, id, values);
            this.props.addOrUpdateInstance(instance);
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    }

    render () {
        return (
            <Settings
                isFetching={ this.state.isFetching }
                isTab={ this.props.isTab }
                onSave={ this.handleSave }
                schema={ this.props.schema }
                values={ this.props.instance }
            />
        );
    }
}

SettingsContainer.propTypes = {
    addOrUpdateInstance: PropTypes.func.isRequired,
    addOrUpdateSchema: PropTypes.func.isRequired,
    instance: PropTypes.objectOf(PropTypes.any),
    isTab: PropTypes.bool,
    router: withRouterPropType,
    schema: PropTypes.objectOf(PropTypes.any)
};

SettingsContainer = connectWithStore(SettingsContainer,
    (state, props) => {
        const [, type, id] = props.router.params;

        const schemas = state.remote.config.realm.secretStores.types.schema;
        const instances = state.remote.config.realm.secretStores.instances.list;
        const instance = find(instances, (inst) => inst._id === id && inst._type._id === type);

        return {
            schema: schemas[type],
            instance
        };
    },
    (dispatch) => ({
        addOrUpdateInstance: bindActionCreators(addOrUpdateInstance, dispatch),
        addOrUpdateSchema: bindActionCreators(addOrUpdateSchema, dispatch)
    })
);

export default withRouter(SettingsContainer);
