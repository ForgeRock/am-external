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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { Form } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import isValidUrlWithPort
    from "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPort";

class NewWebAgentGroupForm extends Component {
    constructor (props) {
        super(props);
        this.state = {
            groupId: "",
            serverUrl: ""
        };
    }

    handleGroupIdChange = (groupId) => {
        this.setState({ groupId }, () => {
            this.props.onGroupIdChange(isValidId(this.state.groupId) ? this.state.groupId : null);
        });
    };

    handleServerUrlChange = (serverUrl) => {
        this.setState({ serverUrl }, () => {
            this.props.onServerUrlChange(isValidUrlWithPort(this.state.serverUrl) ? this.state.serverUrl : null);
        });
    };

    render () {
        const isServerUrlValid = isValidUrlWithPort(this.state.serverUrl);
        const isGroupIdValid = isValidId(this.state.groupId);

        return (
            <Form horizontal>
                <FormGroupInput
                    isValid={ isGroupIdValid }
                    label={ t("console.applications.agents.common.groups.new.groupId.title") }
                    onChange={ this.handleGroupIdChange }
                    validationMessage={ t("console.common.validation.invalidCharacters") }
                    value={ this.state.groupId }
                />
                <FormGroupInput
                    isValid={ isServerUrlValid }
                    label={ t("console.applications.agents.common.serverUrl.title") }
                    onChange={ this.handleServerUrlChange }
                    placeholder={ t("console.applications.agents.common.serverUrl.placeholder") }
                    validationMessage={ t("console.applications.agents.common.serverUrl.validationMessage") }
                    value={ this.state.serverUrl }
                />
                { this.props.children }
            </Form>
        );
    }
}

NewWebAgentGroupForm.propTypes = {
    children: PropTypes.node.isRequired,
    onGroupIdChange: PropTypes.func.isRequired,
    onServerUrlChange: PropTypes.func.isRequired
};

export default NewWebAgentGroupForm;
