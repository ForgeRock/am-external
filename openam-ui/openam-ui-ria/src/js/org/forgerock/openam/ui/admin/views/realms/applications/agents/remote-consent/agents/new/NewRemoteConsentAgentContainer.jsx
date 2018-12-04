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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { setSchema } from "store/modules/remote/config/realm/applications/agents/remoteConsent/agents/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/agents/remoteConsent/agents/template";
import { REMOTE_CONSENT_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewRemoteConsentAgent from "./NewRemoteConsentAgent";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewRemoteConsentAgentContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            agentId: "",
            isFetching: true
        };

        this.handleAgentIdChange = this.handleAgentIdChange.bind(this);
        this.handleCreate = this.handleCreate.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getInitialState(realm, REMOTE_CONSENT_AGENT).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleAgentIdChange (agentId) {
        this.setState({ agentId });
    }

    handleCreate (formData) {
        const realm = this.props.router.params[0];

        create(realm, REMOTE_CONSENT_AGENT, formData, this.state.agentId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsRemoteConsentAgentsEdit,
                { args: map([realm, this.state.agentId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const createAllowed = !isEmpty(this.state.agentId);

        return (
            <NewRemoteConsentAgent
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                onAgentIdChange={ this.handleAgentIdChange }
                onCreate={ this.handleCreate }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewRemoteConsentAgentContainer.propTypes = {
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

NewRemoteConsentAgentContainer = connectWithStore(NewRemoteConsentAgentContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.agents.remoteConsent.agents.schema,
        template: state.remote.config.realm.applications.agents.remoteConsent.agents.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewRemoteConsentAgentContainer = withRouter(NewRemoteConsentAgentContainer);

export default NewRemoteConsentAgentContainer;
