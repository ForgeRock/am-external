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

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { JAVA_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/config/realm/applications/agents/java/groups/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/agents/java/groups/template";
import appendToUrl from "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/appendToUrl";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewJavaAgentGroup from "./NewJavaAgentGroup";
import Router from "org/forgerock/commons/ui/common/main/Router";
import URI from "URI";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewJavaAgentGroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            groupId: ""
        };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleGroupIdChange = this.handleGroupIdChange.bind(this);
        this.handleServerUrlChange = this.handleServerUrlChange.bind(this);
    }

    componentDidMount () {
        getInitialState(this.props.router.params[0], JAVA_AGENT).then(({ schema, values }) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema[0]);
            this.props.setTemplate(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleGroupIdChange (groupId) {
        this.setState({ groupId });
    }

    handleServerUrlChange (serverUrl) {
        this.setState({ serverUrl });
    }

    handleCreate (formData) {
        const realm = this.props.router.params[0];
        const serverUrl = new URI(this.state.serverUrl);
        const updatedValues = {
            ...formData,
            ssoJ2EEAgentConfig: {
                ...formData.ssoJ2EEAgentConfig,
                cdssoUrls: [appendToUrl(this.state.serverUrl, "/cdcservlet")],
                cdssoTrustedIdProvider: [appendToUrl(this.state.serverUrl, "/cdcservlet")]
            },
            amServicesJ2EEAgent: {
                ...formData.amServicesJ2EEAgent,
                amLoginUrl: [appendToUrl(this.state.serverUrl, "/UI/Login")],
                amLogoutUrl: [appendToUrl(this.state.serverUrl, "/UI/Logout")],
                authServiceHost: serverUrl.hostname(),
                authServicePort: parseInt(serverUrl.port(), 10),
                authServiceProtocol: serverUrl.protocol()
            }
        };

        create(realm, JAVA_AGENT, updatedValues, this.state.groupId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsJavaAgentGroupsEdit,
                { args: map([realm, this.state.groupId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const createAllowed = !isEmpty(this.state.groupId) && !isEmpty(this.state.serverUrl);

        return (
            <NewJavaAgentGroup
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                onCreate={ this.handleCreate }
                onGroupIdChange={ this.handleGroupIdChange }
                onServerUrlChange={ this.handleServerUrlChange }
                schema={ this.props.schema }
                serverUrl={ this.state.serverUrl }
                template={ this.props.template }
            />
        );
    }
}

NewJavaAgentGroupContainer.propTypes = {
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

NewJavaAgentGroupContainer = connectWithStore(NewJavaAgentGroupContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.agents.java.groups.schema,
        template: state.remote.config.realm.applications.agents.java.groups.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewJavaAgentGroupContainer = withRouter(NewJavaAgentGroupContainer);

export default NewJavaAgentGroupContainer;