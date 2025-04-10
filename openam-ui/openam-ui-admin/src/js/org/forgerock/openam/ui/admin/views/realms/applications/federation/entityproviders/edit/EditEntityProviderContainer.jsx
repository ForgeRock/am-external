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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    get as getInstance,
    getSchema
} from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import { set as setInstance } from "store/modules/remote/config/realm/applications/federation/entityproviders/instance";
import { set as setSchema } from "store/modules/remote/config/realm/applications/federation/entityproviders/schema";
import connectWithStore from "components/redux/connectWithStore";
import convertFormatToIsPassword from "components/form/schema/convertFormatToIsPassword";
import EditEntityProvider from "./EditEntityProvider";
import Loading from "components/Loading";
import Oops from "components/Oops";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import { getAllByScriptType } from "org/forgerock/openam/ui/admin/services/realm/ScriptsService";
import { getAll as getAllTrees } from "org/forgerock/openam/ui/admin/services/realm/authentication/TreeService";

class EditEntityProviderContainer extends Component {
    static propTypes = {
        router: withRouterPropType,
        schema: PropTypes.objectOf(PropTypes.any),
        setInstance: PropTypes.func.isRequired,
        setSchema: PropTypes.func.isRequired
    };
    state = { fetchError: false, isFetching: true };

    async componentDidMount () {
        const [realm, location,, entityId] = this.props.router.params;
        try {
            const [instance, schema, idpATMScriptsResponse, idpAdapterScripts, spAdapterScripts,
                nameIDMapperScripts, treeNames] = await Promise.all([
                getInstance(realm, location, entityId),
                getSchema(realm, location),
                getAllByScriptType(realm, "SAML2_IDP_ATTRIBUTE_MAPPER"),
                getAllByScriptType(realm, "SAML2_IDP_ADAPTER"),
                getAllByScriptType(realm, "SAML2_SP_ADAPTER"),
                getAllByScriptType(realm, "SAML2_NAMEID_MAPPER"),
                getAllTrees(realm)
            ]);

            /* eslint-disable react/no-did-mount-set-state */
            this.setState({ idpATMScripts: idpATMScriptsResponse.result });
            this.setState({ idpAdapterScripts: idpAdapterScripts.result });
            this.setState({ spAdapterScripts: spAdapterScripts.result });
            this.setState({ nameIDMapperScripts: nameIDMapperScripts.result });
            this.setState({ treeNames: treeNames.result });
            this.props.setInstance(instance);
            this.props.setSchema(convertFormatToIsPassword(schema));
        } catch (error) {
            this.setState({ fetchError: true }); // eslint-disable-line react/no-did-mount-set-state
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    /**
     * Create a JSON Schema for dropdowns.
     * This would contain all the items in the dropdown.
     * @param property property retrieved from the schema
     * @param items list of items to add to the dropdown
     * @param defaultTitle - The default title for the dropdown
     * @returns {{enumNames: *, options: {enum_titles: *}, description, title, type: string, enum: *, exampleValue: string}}
     */
    createDropdownSchema (property, items, defaultTitle) {
        const enums = items.map((item) => { return item._id; });
        enums.unshift("[Empty]");
        const enumTitles = items.map((item) => { return item.name; });
        enumTitles.unshift(defaultTitle);

        return {
            title: property.title,
            description: property.description,
            "enum": enums,
            options: {
                "enum_titles": enumTitles
            },
            enumNames: enumTitles,
            type: "string",
            exampleValue: ""
        };
    }

    render () {
        if (this.state.isFetching) {
            return <Loading />;
        } else if (this.state.fetchError) {
            return <Oops />;
        } else {
            const [, location, role] = this.props.router.params;
            const schema = this.props.schema.properties[role];
            if (location === "hosted") {
                if (role === "identityProvider") {
                    // IDP Attribute Mapper Script
                    const assertionProcessing = schema.properties.assertionProcessing;
                    const atmProperties = assertionProcessing.properties.attributeMapper.properties;
                    /* eslint-disable max-len */
                    atmProperties.attributeMapperScript = this.createDropdownSchema(atmProperties.attributeMapperScript, this.state.idpATMScripts, "--- Select a script ---");
                    // IDP Adapter Script
                    const idpAdapterProperties = schema.properties.advanced.properties.idpAdapter.properties;
                    /* eslint-disable max-len */
                    idpAdapterProperties.idpAdapterScript = this.createDropdownSchema(idpAdapterProperties.idpAdapterScript, this.state.idpAdapterScripts, "--- Select a script ---");
                } else if (role === "serviceProvider") {
                    // SP Adapter Script
                    const spAdapterProperties = schema.properties.assertionProcessing.properties.adapter.properties;
                    /* eslint-disable max-len */
                    spAdapterProperties.spAdapterScript = this.createDropdownSchema(spAdapterProperties.spAdapterScript, this.state.spAdapterScripts, "--- Select a script ---");
                }
            }
            if (location === "remote") {
                if (role === "serviceProvider") {
                    // Name ID Mapper Script
                    const assertionProcessing = schema.properties.assertionProcessing;
                    const accountMapperProperties = assertionProcessing.properties.accountMapper.properties;
                    /* eslint-disable max-len */
                    accountMapperProperties.nameIDMapperScript = this.createDropdownSchema(accountMapperProperties.nameIDMapperScript, this.state.nameIDMapperScripts, "--- Select a script ---");
                    // JTree Configuration
                    /* eslint-disable max-len */
                    const treeConfigurationProperties = schema.properties.advanced.properties.treeConfiguration.properties;
                    treeConfigurationProperties.treeName = this.createDropdownSchema(treeConfigurationProperties.treeName, this.state.treeNames, "--- Select a tree ---");
                }
            }
            return schema
                ? <EditEntityProvider schema={ schema } />
                : <Oops />;
        }
    }
}

EditEntityProviderContainer = connectWithStore(EditEntityProviderContainer,
    (state) => {
        return {
            schema: state.remote.config.realm.applications.federation.entityproviders.schema
        };
    },
    (dispatch) => ({
        setInstance: bindActionCreators(setInstance, dispatch),
        setSchema: bindActionCreators(setSchema, dispatch)
    })
);

EditEntityProviderContainer = withRouter(EditEntityProviderContainer);

export default EditEntityProviderContainer;
