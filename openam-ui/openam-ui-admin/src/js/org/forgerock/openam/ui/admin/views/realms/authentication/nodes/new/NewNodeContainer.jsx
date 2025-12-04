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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
import { isEmpty, map } from "lodash";
import React, { Component } from "react";

import {
    create
} from "org/forgerock/openam/ui/admin/services/global/authentication/NodeDesignerService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewNode from "./NewNode";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewNodeContainer extends Component {
    constructor () {
        super();
        this.state = {
            id: "",
            version: 1,
            name: ""
        };
    }

    handleCreate = () => {
        const realm = this.props.router.params[0];
        create(this.state.id, this.state.name).then((response) => {
            Router.routeTo(Router.configuration.routes.realmsAuthenticationNodesEdit,
                { args: map([realm, response._id], encodeURIComponent), trigger: true });
        }, (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
    };

    handleNameChange = (name) => {
        this.setState({ name });
    };

    handleIdChange = (id) => {
        this.setState({ id });
    };

    render () {
        return (
            <NewNode
                id={ this.state.id }
                isCreateAllowed={ !isEmpty(this.state.name) }
                name={ this.state.name }
                onCreate={ this.handleCreate }
                onIdChange={ this.handleIdChange }
                onNameChange={ this.handleNameChange }
            />
        );
    }
}

NewNodeContainer.propTypes = {
    router: withRouterPropType
};

NewNodeContainer = withRouter(NewNodeContainer);

export default NewNodeContainer;
