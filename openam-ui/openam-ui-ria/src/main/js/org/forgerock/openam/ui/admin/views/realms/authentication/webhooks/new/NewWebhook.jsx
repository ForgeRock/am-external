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

import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import PageHeader from "components/PageHeader";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewWebhook extends Component {
    constructor (props) {
        super(props);
        this.handleCreate = this.handleCreate.bind(this);
    }

    handleCreate () {
        this.props.onCreate(this.props.name);
    }

    render () {
        const isWehbookIdValid = isValidId(this.props.name);
        const content = (
            <div>
                <Form horizontal>
                    <FormGroupInput
                        isValid={ isWehbookIdValid }
                        label={ t("console.authentication.webhooks.new.name") }
                        onChange={ this.props.onNameChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ this.props.name }
                    />
                </Form>
            </div>
        );

        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsAuthenticationWebhooks }
                isCreateAllowed={ this.props.isCreateAllowed }
                onCreateClick={ this.handleCreate }
            />
        );

        return (
            <div>
                <PageHeader title={ t("console.authentication.webhooks.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewWebhook.propTypes = {
    isCreateAllowed: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    onCreate: PropTypes.func.isRequired,
    onNameChange: PropTypes.func.isRequired
};

export default NewWebhook;
