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

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/oauth2/groups/schema";
import { setTemplate } from "store/modules/remote/oauth2/groups/template";
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
                groupId={ this.state.groupId }
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
        schema: state.remote.oauth2.groups.schema,
        template: state.remote.oauth2.groups.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewOAuth2GroupContainer = withRouter(NewOAuth2GroupContainer);

export default NewOAuth2GroupContainer;
