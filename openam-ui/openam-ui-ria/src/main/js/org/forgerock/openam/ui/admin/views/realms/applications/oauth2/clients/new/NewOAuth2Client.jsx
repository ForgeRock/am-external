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

import { cloneDeep } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component, PropTypes } from "react";

import GroupedJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import NewOAuth2ClientFooter from "./NewOAuth2ClientFooter";
import NewOAuth2ClientIdInput from "./NewOAuth2ClientIdInput";
import PageHeader from "components/PageHeader";

class NewOAuth2Client extends Component {
    constructor (props) {
        super(props);
        this.setRef = this.setRef.bind(this);
    }

    componentDidUpdate () {
        /**
         * The properties redirectionUris, scopes/defaultScopes are helpful to users when creating an OAuth2.0 Client,
         * however they are not set as required properties in the schema so they are manually added here.
         */
        const schema = cloneDeep(this.props.schema);
        if (schema) {
            schema.properties.coreOAuth2ClientConfig.defaultProperties = [
                "defaultScopes",
                "redirectionUris",
                "scopes"
            ];
        }
        if (!this.jsonSchemaView && this.props.template) {
            this.jsonSchemaView = new GroupedJSONSchemaView({
                hideInheritance: true,
                schema: new JSONSchema(schema),
                values: new JSONValues(this.props.template),
                showOnlyRequiredAndEmpty: true
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    setRef (element) {
        this.jsonForm = element;
    }

    render () {
        const handleCreate = () => this.props.onCreate(this.jsonSchemaView.getData());

        const footer = (
            <NewOAuth2ClientFooter
                disableCreate={ !this.props.isCreateAllowed }
                onCreateClick={ handleCreate }
            />
        );

        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <div>
                    <NewOAuth2ClientIdInput
                        clientId={ this.props.clientId }
                        onClientIdChange={ this.props.onClientIdChange }
                    />
                    <div ref={ this.setRef } />
                </div>
            );
        }

        return (
            <div>
                <PageHeader title={ t("console.applications.oauth2.clients.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewOAuth2Client.propTypes = {
    clientId: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onClientIdChange: PropTypes.func.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired
};

export default NewOAuth2Client;
