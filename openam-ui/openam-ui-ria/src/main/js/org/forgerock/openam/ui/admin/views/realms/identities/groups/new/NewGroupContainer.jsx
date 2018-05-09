/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { isEmpty, map } from "lodash";
import React, { Component } from "react";

import { create } from "org/forgerock/openam/ui/admin/services/realm/identities/GroupsService";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewGroup from "./NewGroup";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewGroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            id: ""
        };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleIdChange = this.handleIdChange.bind(this);
    }

    handleCreate () {
        const realm = this.props.router.params[0];

        create(realm, this.state.id).then(() => {
            Router.routeTo(Router.configuration.routes.realmsIdentitiesGroupsEdit,
                { args: map([realm, this.state.id], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleIdChange (id) {
        this.setState({ id });
    }

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id);

        return (
            <NewGroup
                id={ this.state.id }
                isCreateAllowed={ createAllowed }
                isValidId={ validId }
                onCreate={ this.handleCreate }
                onIdChange={ this.handleIdChange }
            />
        );
    }
}

NewGroupContainer.propTypes = {
    router: withRouterPropType
};

NewGroupContainer = withRouter(NewGroupContainer);

export default NewGroupContainer;
