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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
import { findKey, flattenDeep, isObject, omit, pickBy, reduce, sortBy, tail } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { remove, update } from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import connectWithStore from "components/redux/connectWithStore";
import isObjectType from "components/form/schema/isObjectType";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditEntityPageHeader from "./EditEntityPageHeader";
import FocusContext from "../context/FocusContext";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const flattenNestedOptionGroups = (property, key, path = [], displayPath = []) => {
    // Unfortunately nested option groups are not yet supported, so here we store the path.
    // @see https://github.com/JedWatson/react-select/pull/275
    if (property.properties) {
        return reduce(property.properties, (result, value, key) => {
            const label = property.title || key;
            result.push(flattenNestedOptionGroups(value, key, [...path, key], [...displayPath, label]));
            return result;
        }, []);
    } else {
        const option = {
            label: property.title || key,
            fullSearchableValue: path.join("."),
            fullSearchableLabel: displayPath.join(" "),
            value: path,
            displayPath: tail(displayPath)
        };
        return option;
    }
};

class EditEntityPageHeaderContainer extends Component {
    static propTypes = {
        instance: PropTypes.objectOf(PropTypes.any),
        router: withRouterPropType,
        schema: PropTypes.objectOf(PropTypes.any)
    };
    state = { searchValue: "" };

    handleAddRole = () => {
        const [realm,,, id] = this.props.router.params;

        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProvidersNewHosted, {
            args: [realm, id].map(encodeURIComponent),
            trigger: true
        });
    };

    handleChangeRole = (role) => {
        const [realm, location,, id] = this.props.router.params;

        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProvidersEdit, {
            args: [realm, location, role, id].map(encodeURIComponent),
            trigger: true
        });
    };

    handleDeleteEntityProvider = async () => {
        const [realm, location,, entityId] = this.props.router.params;

        return remove(realm, location, entityId).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProviders, {
                args: [encodeURIComponent(realm)], trigger: true
            });
        });
    };

    handleDeleteRole = async () => {
        const [realm, location, role, id] = this.props.router.params;
        const body = omit(this.props.instance, role);

        await update(realm, location, id, body);

        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProviders, {
            args: [encodeURIComponent(realm)], trigger: true
        });
    };

    render () {
        const { router, schema } = this.props;
        const [, location, role, entityId] = router.params;
        const subSchema = schema.properties[role].properties;

        const options = schema
            ? sortBy(subSchema, "propertyOrder").map((property) => {
                const key = findKey(subSchema, property);
                const label = property.title || key;
                return {
                    label,
                    options: flattenDeep(flattenNestedOptionGroups(property, key, [key], []))
                };
            })
            : [];

        const schemaRoles = Object.keys(pickBy(this.props.schema.properties, isObjectType));
        const instanceRoles = Object.keys(pickBy(this.props.instance, isObject));
        const disableAddRole = location === "remote" || schemaRoles.every((role) => instanceRoles.includes(role));

        return (
            <FocusContext.Consumer>
                { ({ setPath }) => (
                    <EditEntityPageHeader
                        currentRole={ role }
                        disableAddRole={ disableAddRole }
                        location={ location }
                        onAddRole={ this.handleAddRole }
                        onChangeRole={ this.handleChangeRole }
                        onDeleteEntityProvider={ this.handleDeleteEntityProvider }
                        onDeleteRole={ this.handleDeleteRole }
                        onSearchChange={ setPath }
                        options={ options }
                        roles={ instanceRoles }
                        searchValue={ this.state.searchValue }
                        title={ entityId }
                    />
                )}
            </FocusContext.Consumer>
        );
    }
}

EditEntityPageHeaderContainer = connectWithStore(EditEntityPageHeaderContainer,
    (state) => ({
        instance: state.remote.config.realm.applications.federation.entityproviders.instance,
        schema: state.remote.config.realm.applications.federation.entityproviders.schema
    })
);

EditEntityPageHeaderContainer = withRouter(EditEntityPageHeaderContainer);

export default EditEntityPageHeaderContainer;
