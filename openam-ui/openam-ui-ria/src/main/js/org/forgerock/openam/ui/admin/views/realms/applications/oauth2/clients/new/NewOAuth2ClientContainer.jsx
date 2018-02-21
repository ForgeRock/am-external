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
 * Copyright 2017 ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import React, { Component, PropTypes } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/oauth2/clients/schema";
import { setTemplate } from "store/modules/remote/oauth2/clients/template";
import connectWithStore from "components/redux/connectWithStore";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewOAuth2Client from "./NewOAuth2Client";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewOAuth2ClientContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            clientId: ""
        };

        this.handleClientIdChange = this.handleClientIdChange.bind(this);
    }

    componentDidMount () {
        getInitialState(this.props.router.params[0], OAUTH2_CLIENT).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema[0]);
            this.props.setTemplate(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleClientIdChange (clientId) {
        this.setState({ clientId });
    }

    render () {
        const handleCreate = (formData) => {
            const realm = this.props.router.params[0];
            const values = new JSONValues(formData);
            const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));

            create(realm, OAUTH2_CLIENT, valuesWithoutNullPasswords.raw, this.state.clientId).then(() => {
                Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2ClientsEdit,
                    { args: map([realm, this.state.clientId], encodeURIComponent), trigger: true });
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        };

        return (
            <NewOAuth2Client
                clientId={ this.state.clientId }
                isCreateAllowed={ !isEmpty(this.state.clientId) }
                isFetching={ this.state.isFetching }
                onClientIdChange={ this.handleClientIdChange }
                onCreate={ handleCreate }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewOAuth2ClientContainer.propTypes = {
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

NewOAuth2ClientContainer = connectWithStore(NewOAuth2ClientContainer,
    (state) => ({
        schema: state.remote.oauth2.clients.schema,
        template: state.remote.oauth2.clients.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewOAuth2ClientContainer = withRouter(NewOAuth2ClientContainer);

export default NewOAuth2ClientContainer;
