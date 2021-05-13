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
 * Copyright 2017-2020 ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEqual, map, values } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { setInstances } from "store/modules/remote/config/realm/applications/oauth2/trustedJwtIssuer/agents/instances";
import { TRUSTED_JWT_ISSUER } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import ListTrustedJwtIssuerAgents from "./ListTrustedJwtIssuerAgents";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withPagination, { withPaginationPropTypes }
    from "org/forgerock/openam/ui/admin/views/realms/common/withPagination";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListTrustedJwtIssuerAgentsContainer extends Component {
    constructor () {
        super();

        this.state = { isFetching: true };
    }

    componentDidMount () {
        this.handleTableDataChange(this.props.pagination);
    }

    UNSAFE_componentWillReceiveProps (nextProps) {
        if (!isEqual(this.props.pagination, nextProps.pagination)) {
            this.handleTableDataChange(nextProps.pagination);
        }
    }

    handleTableDataChange = (pagination) => {
        const realm = this.props.router.params[0];
        const additionalParams = {
            fields: ["agentgroup"],
            pagination
        };
        getAll(realm, TRUSTED_JWT_ISSUER, additionalParams).then((response) => {
            this.setState({ isFetching: false });
            this.props.pagination.onDataChange(response);
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    handleDelete = (items) => {
        const ids = items.map((item) => item._id);

        showDeleteDialog({
            names: ids,
            objectName: "agent",
            onConfirm: async () => {
                try {
                    const realm = this.props.router.params[0];
                    await remove(realm, TRUSTED_JWT_ISSUER, ids);
                } catch (error) {
                    Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
                } finally {
                    this.props.pagination.onDataDelete(ids.length);
                }
            }
        });
    };

    handleEdit = (item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsApplicationsOAuth2TrustedJwtIssuerAgentsEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsApplicationsOAuth2TrustedJwtIssuerAgentsNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListTrustedJwtIssuerAgents
                isFetching={ this.state.isFetching }
                items={ this.props.agents }
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

ListTrustedJwtIssuerAgentsContainer.propTypes = {
    agents: PropTypes.arrayOf(PropTypes.object),
    pagination: withPaginationPropTypes,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired
};

ListTrustedJwtIssuerAgentsContainer = connectWithStore(ListTrustedJwtIssuerAgentsContainer,
    (state) => ({
        agents: values(state.remote.config.realm.applications.oauth2.trustedJwtIssuer.agents.instances)
    }),
    (dispatch) => ({
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListTrustedJwtIssuerAgentsContainer = withRouter(ListTrustedJwtIssuerAgentsContainer);
ListTrustedJwtIssuerAgentsContainer = withPagination(ListTrustedJwtIssuerAgentsContainer);

export default ListTrustedJwtIssuerAgentsContainer;
