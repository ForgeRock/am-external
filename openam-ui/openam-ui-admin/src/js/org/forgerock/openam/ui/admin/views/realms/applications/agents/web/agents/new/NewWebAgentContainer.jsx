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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { setSchema } from "store/modules/remote/config/realm/applications/agents/web/agents/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/agents/web/agents/template";
import { WEB_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import connectWithStore from "components/redux/connectWithStore";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewWebAgent from "./NewWebAgent";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewWebAgentContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            agentId: "",
            isFetching: true
        };
    }

    componentDidMount () {
        getInitialState(this.props.router.params[0], WEB_AGENT).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleAgentIdChange = (agentId) => {
        this.setState({ agentId });
    };

    handleAgentUrlChange = (agentUrl) => {
        this.setState({ agentUrl });
    };

    handleServerUrlChange = (serverUrl) => {
        this.setState({ serverUrl });
    };

    handleCreate = (formData) => {
        const realm = this.props.router.params[0];
        const values = new JSONValues(formData);
        const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));
        const updatedValues = {
            ...valuesWithoutNullPasswords.raw,
            agentUrl: this.state.agentUrl,
            serverUrl: this.state.serverUrl
        };

        create(realm, WEB_AGENT, updatedValues, this.state.agentId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsWebAgentsEdit,
                { args: map([realm, this.state.agentId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        const createAllowed = !isEmpty(this.state.agentId) && !isEmpty(this.state.agentUrl) &&
            !isEmpty(this.state.serverUrl);

        return (
            <NewWebAgent
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                onAgentIdChange={ this.handleAgentIdChange }
                onAgentUrlChange={ this.handleAgentUrlChange }
                onCreate={ this.handleCreate }
                onServerUrlChange={ this.handleServerUrlChange }
                schema={ this.props.schema }
                serverUrl={ this.state.serverUrl }
                template={ this.props.template }
            />
        );
    }
}

NewWebAgentContainer.propTypes = {
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

NewWebAgentContainer = connectWithStore(NewWebAgentContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.agents.web.agents.schema,
        template: state.remote.config.realm.applications.agents.web.agents.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewWebAgentContainer = withRouter(NewWebAgentContainer);

export default NewWebAgentContainer;
