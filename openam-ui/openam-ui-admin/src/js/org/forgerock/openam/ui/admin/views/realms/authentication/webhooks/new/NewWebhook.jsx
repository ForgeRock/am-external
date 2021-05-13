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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import PageHeader from "components/PageHeader";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewWebhook extends Component {
    handleCreate = () => {
        this.props.onCreate(this.props.name);
    };

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

        return (
            <div>
                <PageHeader title={ t("console.authentication.webhooks.new.title") } />
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <CreateFooter
                            backRoute={ Router.configuration.routes.realmsAuthenticationWebhooks }
                            disabled={ !this.props.isCreateAllowed }
                            onCreateClick={ this.handleCreate }
                        />
                    </Panel.Footer>
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
