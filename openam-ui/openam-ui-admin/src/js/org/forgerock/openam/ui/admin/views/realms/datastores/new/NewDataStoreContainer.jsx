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
    }

    handleCreate = (formData) => {
        const realm = this.props.router.params[0];

        create(realm, this.state.type, formData, this.state.id).then(() => {
            Router.routeTo(Router.configuration.routes.realmsDataStoresEdit, {
                args: map([
                    realm,
                    this.state.type,
                    this.state.id
                ], encodeURIComponent),
                trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    handleIdChange = (id) => {
        this.setState({ id });
    };

    handleTypeChange = (type) => {
        this.setState({ type, isFetching: true });
        getInitialState(this.props.router.params[0], type).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

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
            />
        );
    }
}

NewDataStoreContainer.propTypes = {
    router: withRouterPropType,
    schema: PropTypes.objectOf(PropTypes.any),
    setSchema: PropTypes.func.isRequired,
    setTemplate: PropTypes.func.isRequired,
    template: PropTypes.objectOf(PropTypes.any)
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
