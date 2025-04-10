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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";
import { isEmpty } from "lodash";

class NewNode extends Component {
    handleCreate = () => {
        this.props.onCreate(this.props.id, this.props.name);
    };

    isValidNodeId = (id) => {
        if (isEmpty(id)) {
            return true;
        }
        return !!id.match(/^[a-z0-9]+$/);
    };

    render () {
        const isNodeIdValid = this.isValidNodeId(this.props.id);
        const content = (
            <div>
                <Form horizontal>
                    <FormGroupInput
                        isValid={ isNodeIdValid }
                        label={ t("console.authentication.nodes.new.id") }
                        onChange={ this.props.onIdChange }
                        validationMessage={ t("console.authentication.nodes.new.invalidCharacters") }
                        value={ this.props.id }
                    />
                    <FormGroupInput
                        label={ t("console.authentication.nodes.new.name") }
                        onChange={ this.props.onNameChange }
                        value={ this.props.name }
                    />
                </Form>
            </div>
        );

        return (
            <div>
                <PageHeader title={ t("console.authentication.nodes.new.title") } />
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <CreateFooter
                            backRoute={ Router.configuration.routes.realmsAuthenticationNodes }
                            disabled={ !this.props.isCreateAllowed }
                            onCreateClick={ this.handleCreate }
                        />
                    </Panel.Footer>
                </Panel>
            </div>
        );
    }
}

NewNode.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    onCreate: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired,
    onNameChange: PropTypes.func.isRequired
};

export default NewNode;
