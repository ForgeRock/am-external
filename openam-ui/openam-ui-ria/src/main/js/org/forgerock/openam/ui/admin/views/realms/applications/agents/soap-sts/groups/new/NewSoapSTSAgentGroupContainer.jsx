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
import { setSchema } from "store/modules/remote/config/realm/applications/agents/soapSts/groups/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/agents/soapSts/groups/template";
import { SOAP_STS_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewSoapSTSAgentGroup from "./NewSoapSTSAgentGroup";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewSoapSTSAgentGroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            groupId: ""
        };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleGroupIdChange = this.handleGroupIdChange.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getInitialState(realm, SOAP_STS_AGENT).then(({ schema, values }) => {
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

    handleCreate (formData) {
        const realm = this.props.router.params[0];

        create(realm, SOAP_STS_AGENT, formData, this.state.groupId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsSoapSTSAgentGroupsEdit,
                { args: map([realm, this.state.groupId], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    render () {
        const createAllowed = !isEmpty(this.state.groupId);

        return (
            <NewSoapSTSAgentGroup
                isCreateAllowed={ createAllowed }
                isFetching={ this.state.isFetching }
                onCreate={ this.handleCreate }
                onGroupIdChange={ this.handleGroupIdChange }
                schema={ this.props.schema }
                serverUrl={ this.state.serverUrl }
                template={ this.props.template }
            />
        );
    }
}

NewSoapSTSAgentGroupContainer.propTypes = {
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

NewSoapSTSAgentGroupContainer = connectWithStore(NewSoapSTSAgentGroupContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.agents.soapSts.groups.schema,
        template: state.remote.config.realm.applications.agents.soapSts.groups.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewSoapSTSAgentGroupContainer = withRouter(NewSoapSTSAgentGroupContainer);

export default NewSoapSTSAgentGroupContainer;
