/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { cloneDeep } from "lodash";
import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import GroupedJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewOAuth2Client extends Component {
    constructor (props) {
        super(props);
        this.state = {
            clientId: ""
        };

        this.handleClientIdChange = this.handleClientIdChange.bind(this);
        this.handleCreate = this.handleCreate.bind(this);
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

    handleCreate () {
        this.props.onCreate(this.jsonSchemaView.getData());
    }

    handleClientIdChange (clientId) {
        this.setState({ clientId }, () => {
            this.props.onClientIdChange(isValidId(this.state.clientId) ? this.state.clientId : null);
        });
    }

    setRef (element) {
        this.jsonForm = element;
    }

    render () {
        const isClientIdValid = isValidId(this.state.clientId);
        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsApplicationsOAuth2 }
                isCreateAllowed={ this.props.isCreateAllowed }
                onCreateClick={ this.handleCreate }
            />
        );
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <Form horizontal>
                    <FormGroupInput
                        isValid={ isClientIdValid }
                        label={ t("console.applications.oauth2.clients.new.clientId") }
                        onChange={ this.handleClientIdChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ this.state.clientId }
                    />
                    <div ref={ this.setRef } />
                </Form>
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
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onClientIdChange: PropTypes.func.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired
};

export default NewOAuth2Client;
