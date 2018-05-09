/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { WEB_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/config/realm/applications/agents/web/groups/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/agents/web/groups/template";
import appendToUrl from "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/appendToUrl";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewWebAgentGroup from "./NewWebAgentGroup";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewWebAgentGroupContainer extends Component {
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
        getInitialState(this.props.router.params[0], WEB_AGENT).then(({ schema, values }) => {
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
        const updatedValues = {
            ...formData,
            ssoWebAgentConfig: {
                cdssoUrls: [appendToUrl(this.state.serverUrl, "/cdcservlet")]
            },
            amServicesWebAgent: {
                ...formData.amServicesWebAgent,
                amLoginUrl: [appendToUrl(this.state.serverUrl, "/UI/Login")],
                amLogoutUrl: [appendToUrl(this.state.serverUrl, "/UI/Logout")]
            }
        };

        create(realm, WEB_AGENT, updatedValues, this.state.groupId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsWebAgentGroupsEdit,
                { args: map([realm, this.state.groupId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const createAllowed = !isEmpty(this.state.groupId) && !isEmpty(this.state.serverUrl);

        return (
            <NewWebAgentGroup
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

NewWebAgentGroupContainer.propTypes = {
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

NewWebAgentGroupContainer = connectWithStore(NewWebAgentGroupContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.agents.web.groups.schema,
        template: state.remote.config.realm.applications.agents.web.groups.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewWebAgentGroupContainer = withRouter(NewWebAgentGroupContainer);

export default NewWebAgentGroupContainer;
