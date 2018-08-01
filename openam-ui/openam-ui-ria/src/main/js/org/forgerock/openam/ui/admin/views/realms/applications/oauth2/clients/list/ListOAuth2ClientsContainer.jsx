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
 * Copyright 2017-2018 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { isEqual, map, pluck, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { setInstances } from "store/modules/remote/config/realm/applications/oauth2/clients/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListOAuth2Clients from "./ListOAuth2Clients";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withPagination, { withPaginationPropTypes }
    from "org/forgerock/openam/ui/admin/views/realms/common/withPagination";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListOAuth2ClientsContainer extends Component {
    constructor () {
        super();

        this.handleDelete = this.handleDelete.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleTableDataChange = this.handleTableDataChange.bind(this);

        this.state = { isFetching: true };
    }

    componentDidMount () {
        this.handleTableDataChange(this.props.pagination);
    }

    componentWillReceiveProps (nextProps) {
        if (!isEqual(this.props.pagination, nextProps.pagination)) {
            this.handleTableDataChange(nextProps.pagination);
        }
    }

    handleEdit (item) {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2ClientsEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    }

    handleTableDataChange (pagination) {
        const realm = this.props.router.params[0];
        const additionalParams = {
            fields: ["coreOAuth2ClientConfig/status", "coreOAuth2ClientConfig/agentgroup"],
            pagination
        };
        getAll(realm, OAUTH2_CLIENT, additionalParams).then((response) => {
            this.setState({ isFetching: false });
            this.props.pagination.onDataChange(response);
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleDelete (items) {
        const ids = pluck(items, "_id");
        const realm = this.props.router.params[0];

        showConfirmationBeforeAction({
            message: t("console.applications.oauth2.clients.confirmDeleteSelected", {
                count: ids.length
            })
        }, () => {
            remove(realm, OAUTH2_CLIENT, ids).then(() => {
                Messages.messages.displayMessageFromConfig("changesSaved");
                this.props.pagination.onDataDelete(ids.length);
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                this.props.pagination.onDataDelete(ids.length);
            });
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsApplicationsOAuth2ClientsNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListOAuth2Clients
                isFetching={ this.state.isFetching }
                items={ this.props.clients }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
                options={ {
                    ...this.props.pagination
                } }
            />
        );
    }
}

ListOAuth2ClientsContainer.propTypes = {
    clients: PropTypes.arrayOf(PropTypes.object),
    pagination: withPaginationPropTypes,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired
};

ListOAuth2ClientsContainer = connectWithStore(ListOAuth2ClientsContainer,
    (state) => ({
        clients: values(state.remote.config.realm.applications.oauth2.clients.instances)
    }),
    (dispatch) => ({
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListOAuth2ClientsContainer = withRouter(ListOAuth2ClientsContainer);
ListOAuth2ClientsContainer = withPagination(ListOAuth2ClientsContainer);

export default ListOAuth2ClientsContainer;