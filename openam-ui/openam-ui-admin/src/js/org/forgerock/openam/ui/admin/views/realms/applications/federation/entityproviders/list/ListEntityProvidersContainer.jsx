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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { forEach, isEmpty, isEqual, keyBy, map, values } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    remove,
    searchEntities
} from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import { remove as removeInstance, set as setInstances }
    from "store/modules/remote/config/realm/applications/federation/entityproviders/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListEntityProviders from "./ListEntityProviders";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withPagination, { withPaginationPropTypes }
    from "org/forgerock/openam/ui/admin/views/realms/common/withPagination";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListEntityProvidersContainer extends Component {
    static propTypes = {
        instances: PropTypes.arrayOf(PropTypes.object),
        pagination: withPaginationPropTypes,
        removeInstance: PropTypes.func.isRequired,
        router: withRouterPropType,
        setInstances: PropTypes.func.isRequired
    };
    state = { isFetching: true };

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
            fields: [],
            pagination
        };

        searchEntities(realm, additionalParams).then((response) => {
            this.setState({ isFetching: false });
            this.props.pagination.onDataChange(response);
            this.props.setInstances(keyBy(response.result, "_id"));
        }, () => {
            this.setState({ isFetching: false });
        });
    };

    handleEdit = (e, { entityId, location, roles }) => {
        const realm = this.props.router.params[0];
        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProvidersEdit, {
            args: map([realm, location, roles[0], entityId], encodeURIComponent),
            trigger: true
        });
    };

    handleDelete = (items) => {
        const realm = this.props.router.params[0];
        const promises = items.map(({ entityId, location }) => remove(realm, location, entityId));

        return Promise.all(promises).then((response) => {
            forEach(response, ({ _id }) => this.props.removeInstance(_id));
        });
    };

    render () {
        const content = this.state.isFetching
            ? <Loading />
            : (
                <ListEntityProviders
                    isSearching={ !isEmpty(this.props.pagination.searchTerm) }
                    items={ this.props.instances }
                    onDelete={ this.handleDelete }
                    onRowClick={ this.handleEdit }
                    onSearchKeyPress={ this.props.pagination.handleSearchChange }
                    options={ {
                        ...this.props.pagination
                    } }
                    searchTerm={ this.props.pagination.searchTerm }
                />
            );

        return (
            <>
                <PageHeader title={ t("console.applications.federation.entityProviders.list.title") } />
                <Panel>
                    <Panel.Body>
                        { content }
                    </Panel.Body>
                </Panel>
            </>
        );
    }
}

ListEntityProvidersContainer = connectWithStore(ListEntityProvidersContainer,
    (state) => ({
        instances: values(state.remote.config.realm.applications.federation.entityproviders.instances)
    }),
    (dispatch) => ({
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListEntityProvidersContainer = withRouter(ListEntityProvidersContainer);
ListEntityProvidersContainer = withPagination(ListEntityProvidersContainer);

export default ListEntityProvidersContainer;
