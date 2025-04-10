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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { bindActionCreators } from "redux";
import { get, isEmpty, map } from "lodash";
import { Grid } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    create,
    getCreatableTypes,
    getInitialState
} from "org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoresService";
import { addOrUpdate as addOrUpdateSchema } from "store/modules/remote/config/global/secretStores/types/schema";
import { addOrUpdate as addOrUpdateTemplate } from "store/modules/remote/config/global/secretStores/types/template";
import {
    reset as resetSelected,
    set as setSelected
} from "store/modules/local/config/global/secretStores/types/selected";
import { set as setTypes } from "store/modules/remote/config/global/secretStores/types/list";
import connectWithStore from "components/redux/connectWithStore";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewSecretStores from "org/forgerock/openam/ui/admin/views/common/secretStores/new/NewSecretStores";
import Router from "org/forgerock/commons/ui/common/main/Router";
import setHSMRequiredProperties from
    "org/forgerock/openam/ui/admin/views/common/secretStores/new/setHSMRequiredProperties";

class NewGlobalSecretStoresContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            id: "",
            isFetching: true
        };
    }

    async componentDidMount () {
        this.props.resetSelected();

        try {
            const { result } = await getCreatableTypes();
            this.props.setTypes(result);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleCreate = async (formData) => {
        try {
            await create(this.props.selectedType, formData, this.state.id);
            Router.routeTo(Router.configuration.routes.editGlobalSecretStores, {
                args: map([
                    this.props.selectedType,
                    this.state.id
                ], encodeURIComponent),
                trigger: true
            });
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    };

    handleIdChange = (id) => {
        this.setState({ id });
    };

    handleTypeChange = async (type) => {
        this.props.setSelected(type);

        try {
            const { schema, values } = await getInitialState(type);
            const modifiedSchema = setHSMRequiredProperties(schema, type);
            this.props.addOrUpdateSchema(modifiedSchema, type);
            this.props.addOrUpdateTemplate(values, type);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    };

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id) && !isEmpty(this.props.selectedType);

        return (
            <Grid>
                <NewSecretStores
                    id={ this.state.id }
                    isCreateAllowed={ createAllowed }
                    isFetching={ this.state.isFetching }
                    isValidId={ validId }
                    listRoute={ Router.configuration.routes.listGlobalSecretStores }
                    onCreate={ this.handleCreate }
                    onIdChange={ this.handleIdChange }
                    onTypeChange={ this.handleTypeChange }
                    schema={ this.props.schema }
                    selectedType={ this.props.selectedType }
                    template={ this.props.template }
                    types={ this.props.types }
                />
            </Grid>
        );
    }
}

NewGlobalSecretStoresContainer.propTypes = {
    addOrUpdateSchema: PropTypes.func.isRequired,
    addOrUpdateTemplate: PropTypes.func.isRequired,
    resetSelected: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    selectedType: PropTypes.string,
    setSelected: PropTypes.func.isRequired,
    setTypes: PropTypes.func.isRequired,
    template: PropTypes.objectOf(PropTypes.any),
    types: PropTypes.arrayOf(PropTypes.object).isRequired
};

NewGlobalSecretStoresContainer = connectWithStore(NewGlobalSecretStoresContainer,
    (state) => {
        const selectedType = state.local.config.global.secretStores.types.selected;

        return {
            schema: get(state.remote.config.global.secretStores.types.schema, [selectedType]),
            selectedType,
            template: get(state.remote.config.global.secretStores.types.template, [selectedType]),
            types: state.remote.config.global.secretStores.types.list
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

export default NewGlobalSecretStoresContainer;
