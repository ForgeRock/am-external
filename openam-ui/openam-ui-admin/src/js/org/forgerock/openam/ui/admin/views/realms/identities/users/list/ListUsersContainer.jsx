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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { isEmpty, isEqual, map, values } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { searchUsers, remove } from "org/forgerock/openam/ui/admin/services/realm/identities/UsersService";
import { setInstances } from "store/modules/remote/config/realm/identities/users/instances";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import connectWithStore from "components/redux/connectWithStore";
import ListUsers from "./ListUsers";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withPagination, { withPaginationPropTypes }
    from "org/forgerock/openam/ui/admin/views/realms/common/withPagination";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListUsersContainer extends Component {
    static propTypes = {
        identities: PropTypes.arrayOf(PropTypes.object),
        pagination: withPaginationPropTypes,
        router: withRouterPropType,
        setInstances: PropTypes.func.isRequired
    };

    state = {
        isFetching: true
    };

    componentDidMount () {
        this.handleTableDataChange(this.props.pagination);
    }

    componentDidUpdate (prevProps) {
        if (!isEqual(this.props.pagination, prevProps.pagination)) {
            this.handleTableDataChange(this.props.pagination);
        }
    }

    handleDelete = (items) => {
        const ids = items.map((item) => item._id);

        showDeleteDialog({
            names: ids,
            objectName: "identity",
            onConfirm: async () => {
                try {
                    const realm = this.props.router.params[0];
                    await remove(realm, ids);
                } catch (error) {
                    Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
                } finally {
                    this.props.pagination.onDataDelete(ids.length);
                }
            }
        });
    };

    handleEdit = (e, item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsIdentitiesUsersEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    handleTableDataChange = (pagination) => {
        const realm = this.props.router.params[0];
        const additionalParams = {
            fields: ["cn", "mail", "username", "inetUserStatus"],
            pagination
        };
        searchUsers(realm, additionalParams).then((response) => {
            this.setState({ isFetching: false });
            this.props.pagination.onDataChange(response);
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsIdentitiesUsersNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListUsers
                isFetching={ this.state.isFetching }
                isSearching={ !isEmpty(this.props.pagination.searchTerm) }
                items={ this.props.identities }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
                onSearchKeyPress={ this.props.pagination.handleSearchChange }
                options={ {
                    ...this.props.pagination
                } }
                searchTerm={ this.props.pagination.searchTerm }
            />
        );
    }
}

ListUsersContainer = connectWithStore(ListUsersContainer,
    (state) => ({
        identities: values(state.remote.config.realm.identities.users.instances)
    }),
    (dispatch) => ({
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListUsersContainer = withRouter(ListUsersContainer);
ListUsersContainer = withPagination(ListUsersContainer);

export default ListUsersContainer;
