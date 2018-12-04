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
import { get, isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    create,
    getCreatableTypes,
    getInitialState
} from "org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoresService";
import { addOrUpdate as addOrUpdateSchema } from "store/modules/remote/config/realm/secretStores/types/schema";
import { addOrUpdate as addOrUpdateTemplate } from "store/modules/remote/config/realm/secretStores/types/template";
import {
    reset as resetSelected,
    set as setSelected
} from "store/modules/local/config/realm/secretStores/types/selected";
import { set as setTypes } from "store/modules/remote/config/realm/secretStores/types/list";
import connectWithStore from "components/redux/connectWithStore";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewSecretStores from "org/forgerock/openam/ui/admin/views/common/secretStores/new/NewSecretStores";
import Router from "org/forgerock/commons/ui/common/main/Router";
import setHSMRequiredProperties from
    "org/forgerock/openam/ui/admin/views/common/secretStores/new/setHSMRequiredProperties";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewSecretStoresContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            id: "",
            isFetching: true
        };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleIdChange = this.handleIdChange.bind(this);
        this.handleTypeChange = this.handleTypeChange.bind(this);
    }

    async componentDidMount () {
        this.props.resetSelected();

        const realm = this.props.router.params[0];

        try {
            const { result } = await getCreatableTypes(realm);
            this.props.setTypes(result);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    async handleCreate (formData) {
        const realm = this.props.router.params[0];

        try {
            await create(realm, this.props.selectedType, formData, this.state.id);
            Router.routeTo(Router.configuration.routes.realmsSecretStoresEdit, {
                args: map([
                    realm,
                    this.props.selectedType,
                    this.state.id
                ], encodeURIComponent),
                trigger: true
            });
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    }

    handleIdChange (id) {
        this.setState({ id });
    }

    async handleTypeChange (type) {
        this.props.setSelected(type);

        const realm = this.props.router.params[0];

        try {
            const { schema, values } = await getInitialState(realm, type);
            const modifiedSchema = setHSMRequiredProperties(schema, type);
            this.props.addOrUpdateSchema(modifiedSchema, type);
            this.props.addOrUpdateTemplate(values, type);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    }

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id) && !isEmpty(this.props.selectedType);

        return (
            <NewSecretStores
                id={ this.state.id }
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                isValidId={ validId }
                listRoute={ Router.configuration.routes.realmsSecretStores }
                onCreate={ this.handleCreate }
                onIdChange={ this.handleIdChange }
                onTypeChange={ this.handleTypeChange }
                schema={ this.props.schema }
                selectedType={ this.props.selectedType }
                template={ this.props.template }
                types={ this.props.types }
            />
        );
    }
}

NewSecretStoresContainer.propTypes = {
    addOrUpdateSchema: PropTypes.func.isRequired,
    addOrUpdateTemplate: PropTypes.func.isRequired,
    resetSelected: PropTypes.func.isRequired,
    router: withRouterPropType,
    schema: PropTypes.objectOf(PropTypes.any),
    selectedType: PropTypes.string,
    setSelected: PropTypes.func.isRequired,
    setTypes: PropTypes.func.isRequired,
    template: PropTypes.objectOf(PropTypes.any),
    types: PropTypes.arrayOf(PropTypes.object).isRequired
};

NewSecretStoresContainer = connectWithStore(NewSecretStoresContainer,
    (state) => {
        const selectedType = state.local.config.realm.secretStores.types.selected;

        return {
            schema: get(state.remote.config.realm.secretStores.types.schema, [selectedType]),
            selectedType,
            template: get(state.remote.config.realm.secretStores.types.template, [selectedType]),
            types: state.remote.config.realm.secretStores.types.list
        };
    },
    (dispatch) => ({
        addOrUpdateSchema: bindActionCreators(addOrUpdateSchema, dispatch),
        addOrUpdateTemplate: bindActionCreators(addOrUpdateTemplate, dispatch),
        resetSelected: bindActionCreators(resetSelected, dispatch),
        setSelected: bindActionCreators(setSelected, dispatch),
        setTypes: bindActionCreators(setTypes, dispatch)
    })
);
NewSecretStoresContainer = withRouter(NewSecretStoresContainer);

export default NewSecretStoresContainer;
