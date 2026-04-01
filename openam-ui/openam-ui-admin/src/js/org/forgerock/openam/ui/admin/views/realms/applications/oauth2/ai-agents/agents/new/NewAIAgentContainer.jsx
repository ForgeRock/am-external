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
 * Copyright 2026 Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { AI_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/config/realm/applications/oauth2/clients/clients/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/oauth2/clients/clients/template";
import connectWithStore from "components/redux/connectWithStore";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewAIAgent from "./NewAIAgent";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewAIAgentContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            clientId: ""
        };
    }

    componentDidMount () {
        getInitialState(this.props.router.params[0], AI_AGENT).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleClientIdChange = (clientId) => {
        this.setState({ clientId });
    };

    handleCreate = (formData) => {
        const realm = this.props.router.params[0];
        const values = new JSONValues(formData);
        const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));

        create(realm, AI_AGENT, valuesWithoutNullPasswords.raw, this.state.clientId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2AIAgentsAgentEdit,
                { args: map([realm, this.state.clientId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        return (
            <NewAIAgent
                isCreateAllowed={ !isEmpty(this.state.clientId) }
                isFetching={ this.state.isFetching }
                onClientIdChange={ this.handleClientIdChange }
                onCreate={ this.handleCreate }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewAIAgentContainer.propTypes = {
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

NewAIAgentContainer = connectWithStore(NewAIAgentContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.oauth2.clients.clients.schema,
        template: state.remote.config.realm.applications.oauth2.clients.clients.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewAIAgentContainer = withRouter(NewAIAgentContainer);

export default NewAIAgentContainer;
