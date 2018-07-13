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
import { first, forEach, map, values } from "lodash";
import { t } from "i18next";
import React, { Component, PropTypes } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { removeInstance, setInstances } from "store/modules/remote/oauth2/clients/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListOAuth2Clients from "./ListOAuth2Clients";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListOAuth2ClientsContainer extends Component {
    constructor () {
        super();
        this.onDelete = this.onDelete.bind(this);
        this.state = { isFetching: true };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        getAll(realm, OAUTH2_CLIENT).then((response) => {
            this.setState({ isFetching: false });
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    onDelete (ids) {
        const realm = this.props.router.params[0];

        remove(realm, OAUTH2_CLIENT, ids)
            .then((response) => map(response, first))
            .then((response) => {
                Messages.messages.displayMessageFromConfig("changesSaved");

                forEach(response, this.props.removeInstance);
            }, (reason) => {
                Messages.addMessage({ reason, type: Messages.TYPE_DANGER });
            });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsApplicationsOAuth2ClientsNew, [
            encodeURIComponent(realm)
        ]);
        const handleEdit = (id) => Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2ClientsEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
        const handleDelete = (ids) => {
            showConfirmationBeforeAction({
                message: t("console.applications.oauth2.clients.confirmDeleteSelected", {
                    count: ids.length
                })
            }, () => {
                remove(realm, OAUTH2_CLIENT, ids)
                    .then((response) => map(response, first))
                    .then((response) => {
                        Messages.messages.displayMessageFromConfig("changesSaved");

                        forEach(response, this.props.removeInstance);
                    }, (reason) => {
                        Messages.addMessage({ reason, type: Messages.TYPE_DANGER });
                    });
            });
        };
        return (
            <ListOAuth2Clients
                clients={ this.props.clients }
                isFetching={ this.state.isFetching }
                newHref={ `#${newHref}` }
                onDelete={ handleDelete }
                onEdit={ handleEdit }
            />
        );
    }
}

ListOAuth2ClientsContainer.propTypes = {
    clients: PropTypes.arrayOf(PropTypes.object),
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired
};

ListOAuth2ClientsContainer = connectWithStore(ListOAuth2ClientsContainer,
    (state) => ({
        clients: values(state.remote.oauth2.clients.instances)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListOAuth2ClientsContainer = withRouter(ListOAuth2ClientsContainer);

export default ListOAuth2ClientsContainer;
