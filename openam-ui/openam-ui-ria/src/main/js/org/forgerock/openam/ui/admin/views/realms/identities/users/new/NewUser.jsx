/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewUser extends Component {
    constructor () {
        super();

        this.handleCreate = this.handleCreate.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.template) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                hideInheritance: true,
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.template),
                showOnlyRequiredAndEmpty: true
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    handleCreate () {
        this.props.onCreate(this.jsonSchemaView.getData());
    }

    setRef (element) {
        this.jsonForm = element;
    }

    render () {
        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsIdentities }
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
                        isValid={ this.props.isValidId }
                        label={ t("console.common.id") }
                        onChange={ this.props.onIdChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ this.props.id }
                    />
                    <div ref={ this.setRef } />
                    <FormGroupInput
                        label={ t("common.user.emailAddress") }
                        onChange={ this.props.onEmailChange }
                    />
                </Form>
            );
        }

        return (
            <div>
                <PageHeader title={ t("console.identities.users.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewUser.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    isValidId: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onEmailChange: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired
};

export default NewUser;
