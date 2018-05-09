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
import { findWhere, get, map, result } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAllTypes, getSchema, getTemplate, create }
    from "org/forgerock/openam/ui/admin/services/realm/identities/UsersServicesService";
import { setSchema } from "store/modules/remote/config/realm/identities/users/services/schema";
import { setTemplate } from "store/modules/remote/config/realm/identities/users/services/template";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewUserService from "./NewUserService";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewUserServiceContainer extends Component {
    constructor () {
        super();
        this.state = { isFetching: false };
        this.handleCreate = this.handleCreate.bind(this);
    }

    componentDidMount () {
        const [realm, userId, serviceId] = this.props.router.params;

        Promise.all([
            getSchema(realm, serviceId, userId),
            getTemplate(realm, serviceId, userId),
            getAllTypes(realm, userId)
        ])
            .then(([schema, template, serviceTypes]) => {
                this.props.setTemplate(template[0], serviceId);
                this.props.setSchema(schema[0], serviceId);
                this.setState({
                    type: result(findWhere(serviceTypes, { "_id": serviceId }), "name"),
                    isFetching: false
                });
            }, (response) => {
                this.setState({ isFetching: false });
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
    }

    handleCreate (formData) {
        const [realm, userId, serviceId] = this.props.router.params;

        create(realm, userId, serviceId, formData).then(() => {
            Router.routeTo(Router.configuration.routes.realmsIdentitiesUsersServicesEdit,
                { args: map([realm, userId, serviceId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const userId = this.props.router.params[1];

        return (
            <NewUserService
                id={ userId }
                isFetching={ this.state.isFetching }
                onCreate={ this.handleCreate }
                schema={ this.props.schema }
                template={ this.props.template }
                type={ this.state.type }
            />
        );
    }
}

NewUserServiceContainer.propTypes = {
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

NewUserServiceContainer = connectWithStore(NewUserServiceContainer,
    (state, props) => {
        const serviceId = props.router.params[2];

        return {
            schema: get(state.remote.config.realm.identities.users.services.schema, serviceId),
            template: get(state.remote.config.realm.identities.users.services.template, serviceId)
        };
    },
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewUserServiceContainer = withRouter(NewUserServiceContainer);

export default NewUserServiceContainer;
