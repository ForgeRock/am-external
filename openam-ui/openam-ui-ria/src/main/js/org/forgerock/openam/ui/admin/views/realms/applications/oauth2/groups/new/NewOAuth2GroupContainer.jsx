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
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/config/realm/applications/oauth2/groups/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/oauth2/groups/template";
import connectWithStore from "components/redux/connectWithStore";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewOAuth2Group from "./NewOAuth2Group";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewOAuth2GroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            groupId: ""
        };

        this.handleGroupIdChange = this.handleGroupIdChange.bind(this);
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

    handleGroupIdChange (groupId) {
        this.setState({ groupId });
    }

    render () {
        const handleCreate = (formData) => {
            const realm = this.props.router.params[0];
            create(realm, OAUTH2_CLIENT, formData, this.state.groupId).then(() => {
                Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2GroupsEdit,
                    { args: map([realm, this.state.groupId], encodeURIComponent), trigger: true });
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        };

        return (
            <NewOAuth2Group
                isCreateAllowed={ !isEmpty(this.state.groupId) }
                isFetching={ this.state.isFetching }
                onCreate={ handleCreate }
                onGroupIdChange={ this.handleGroupIdChange }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewOAuth2GroupContainer.propTypes = {
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

NewOAuth2GroupContainer = connectWithStore(NewOAuth2GroupContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.oauth2.groups.schema,
        template: state.remote.config.realm.applications.oauth2.groups.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewOAuth2GroupContainer = withRouter(NewOAuth2GroupContainer);

export default NewOAuth2GroupContainer;
