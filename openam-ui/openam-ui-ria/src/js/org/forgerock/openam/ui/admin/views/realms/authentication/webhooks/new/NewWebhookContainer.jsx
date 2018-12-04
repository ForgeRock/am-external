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
 * Copyright 2018 ForgeRock AS.
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
