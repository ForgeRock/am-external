/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { isEmpty, map } from "lodash";
import React, { Component } from "react";

import {
    create
} from "org/forgerock/openam/ui/admin/services/realm/authentication/WebhookService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewWebhook from "./NewWebhook";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewWebhookContainer extends Component {
    constructor () {
        super();
        this.state = {
            name: ""
        };
        this.handleCreate = this.handleCreate.bind(this);
        this.handleNameChange = this.handleNameChange.bind(this);
    }

    handleCreate () {
        const realm = this.props.router.params[0];
        create(realm, this.state.name).then(() => {
            Router.routeTo(Router.configuration.routes.realmsAuthenticationWebhooksEdit,
                { args: map([realm, this.state.name], encodeURIComponent), trigger: true });
        }, (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
    }

    handleNameChange (name) {
        this.setState({ name });
    }

    render () {
        return (
            <NewWebhook
                isCreateAllowed={ !isEmpty(this.state.name) }
                name={ this.state.name }
                onCreate={ this.handleCreate }
                onNameChange={ this.handleNameChange }
            />
        );
    }
}

NewWebhookContainer.propTypes = {
    router: withRouterPropType
};

NewWebhookContainer = withRouter(NewWebhookContainer);

export default NewWebhookContainer;
