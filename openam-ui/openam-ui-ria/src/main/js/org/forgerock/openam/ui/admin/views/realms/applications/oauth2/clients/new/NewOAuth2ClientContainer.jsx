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

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setSchema } from "store/modules/remote/config/realm/applications/oauth2/clients/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/oauth2/clients/template";
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
        schema: state.remote.config.realm.applications.oauth2.clients.schema,
        template: state.remote.config.realm.applications.oauth2.clients.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewOAuth2ClientContainer = withRouter(NewOAuth2ClientContainer);

export default NewOAuth2ClientContainer;
