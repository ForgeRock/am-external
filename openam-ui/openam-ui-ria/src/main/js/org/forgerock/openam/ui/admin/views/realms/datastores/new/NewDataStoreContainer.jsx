/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import { setSchema } from "store/modules/remote/config/realm/datastores/schema";
import { setTemplate } from "store/modules/remote/config/realm/datastores/template";
import connectWithStore from "components/redux/connectWithStore";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewDataStore from "./NewDataStore";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewDataStoreContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            id: "",
            type: "",
            isFetching: false
        };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleIdChange = this.handleIdChange.bind(this);
        this.handleTypeChange = this.handleTypeChange.bind(this);
    }

    handleCreate (formData) {
        const realm = this.props.router.params[0];

        create(realm, this.state.type, formData, this.state.id).then(() => {
            Router.routeTo(Router.configuration.routes.realmsDataStoresEdit,
                { args: map([
                    realm,
                    this.state.type,
                    this.state.id
                ], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleIdChange (id) {
        this.setState({ id });
    }

    handleTypeChange (type) {
        this.setState({ type, isFetching: true });
        getInitialState(this.props.router.params[0], type).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema[0]);
            this.props.setTemplate(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id) && !isEmpty(this.state.type);

        return (
            <NewDataStore
                id={ this.state.id }
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                isValidId={ validId }
                onCreate={ this.handleCreate }
                onIdChange={ this.handleIdChange }
                onTypeChange={ this.handleTypeChange }
                schema={ this.props.schema }
                template={ this.props.template }
                type={ this.state.type }
            />
        );
    }
}

NewDataStoreContainer.propTypes = {
    router: withRouterPropType,
    schema: PropTypes.shape({
        type: PropTypes.string.isRequired
    }),
    setSchema: PropTypes.func.isRequired,
    setTemplate: PropTypes.func.isRequired,
    template: PropTypes.shape({
        type: PropTypes.string.isRequired
    })
};

NewDataStoreContainer = connectWithStore(NewDataStoreContainer,
    (state) => ({
        schema: state.remote.config.realm.datastores.schema,
        template: state.remote.config.realm.datastores.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewDataStoreContainer = withRouter(NewDataStoreContainer);

export default NewDataStoreContainer;
