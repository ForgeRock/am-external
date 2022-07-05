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
 * Copyright 2021 ForgeRock AS.
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
            const [instance, schema, idpATMScriptsResponse, idpAdapterScripts] = await Promise.all([
                getInstance(realm, location, entityId),
                getSchema(realm, location),
                getAllByScriptType(realm, "SAML2_IDP_ATTRIBUTE_MAPPER"),
                getAllByScriptType(realm, "SAML2_IDP_ADAPTER")
            ]);

            /* eslint-disable react/no-did-mount-set-state */
            this.setState({ idpATMScripts: idpATMScriptsResponse.result });
            this.setState({ idpAdapterScripts: idpAdapterScripts.result });
            this.props.setInstance(instance);
            this.props.setSchema(convertFormatToIsPassword(schema));
        } catch (error) {
            this.setState({ fetchError: true }); // eslint-disable-line react/no-did-mount-set-state
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    /**
     * Create a JSON Schema for scripts.
     * This would contain all the realm scripts.
     * @param scriptProperty a script property retrieved from the schema
     * @param scripts a list of scripts retrieved from the scripts API
     * @returns {{enumNames, options: {enum_titles}, description, title, type: string, enum, exampleValue: string}}
     */
    createScriptsSchema (scriptProperty, scripts) {
        const enums = scripts.map((item) => { return item._id; });
        enums.push("[Empty]");
        const enumTitles = scripts.map((item) => { return item.name; });
        enumTitles.push("--- Select a script ---");

        return {
            title: scriptProperty.title,
            description: scriptProperty.description,
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
            // TODO: Review role condition on SP implementation
            if (role === "identityProvider" && location === "hosted") {
                // IDP Attribute Mapper Script
                const assertionProcessing = schema.properties.assertionProcessing;
                const atmProperties = assertionProcessing.properties.attributeMapper.properties;
                /* eslint-disable max-len */
                atmProperties.attributeMapperScript = this.createScriptsSchema(atmProperties.attributeMapperScript, this.state.idpATMScripts);
                // IDP Adapter Script
                const idpAdapterProperties = schema.properties.advanced.properties.idpAdapter.properties;
                /* eslint-disable max-len */
                idpAdapterProperties.idpAdapterScript = this.createScriptsSchema(idpAdapterProperties.idpAdapterScript, this.state.idpAdapterScripts);
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
