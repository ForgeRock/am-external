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
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/identities/UsersService";
import { setSchema } from "store/modules/remote/config/realm/identities/users/schema";
import { setTemplate } from "store/modules/remote/config/realm/identities/users/template";
import connectWithStore from "components/redux/connectWithStore";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewUser from "./NewUser";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewUserContainer extends Component {
    constructor () {
        super();

        this.state = {
            isFetching: true,
            id: "",
            email: ""
        };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        getInitialState(realm).then(({ schema, values }) => {
            this.setState({
                isFetching: false
            });
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleEmailChange = (email) => {
        this.setState({ email });
    };

    handleIdChange = (id) => {
        this.setState({ id });
    };

    handleCreate = (formData) => {
        const realm = this.props.router.params[0];
        const values = new JSONValues(formData);
        const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));
        const valuesWithEmail = { ...valuesWithoutNullPasswords.raw, mail: this.state.email };

        create(realm, valuesWithEmail, this.state.id).then(() => {
            Router.routeTo(Router.configuration.routes.realmsIdentitiesUsersEdit,
                { args: map([realm, this.state.id], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id);

        return (
            <NewUser
                id={ this.state.id }
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                isValidId={ validId }
                onCreate={ this.handleCreate }
                onEmailChange={ this.handleEmailChange }
                onIdChange={ this.handleIdChange }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewUserContainer.propTypes = {
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

NewUserContainer = connectWithStore(NewUserContainer,
    (state) => ({
        schema: state.remote.config.realm.identities.users.schema,
        template: state.remote.config.realm.identities.users.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewUserContainer = withRouter(NewUserContainer);

export default NewUserContainer;
