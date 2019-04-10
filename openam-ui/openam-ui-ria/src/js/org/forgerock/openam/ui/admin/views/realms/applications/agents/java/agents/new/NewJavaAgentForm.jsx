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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { Form } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import isValidUrlWithPort
    from "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPort";
import isValidUrlWithPortAndPath
    from "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/isValidUrlWithPortAndPath";

class NewJavaAgentForm extends Component {
    constructor (props) {
        super(props);
        this.state = {
            agentId: "",
            agentUrl: "",
            serverUrl: ""
        };
        this.handleAgentIdChange = this.handleAgentIdChange.bind(this);
        this.handleAgentUrlChange = this.handleAgentUrlChange.bind(this);
        this.handleServerUrlChange = this.handleServerUrlChange.bind(this);
    }

    handleAgentIdChange (agentId) {
        this.setState({ agentId }, () => {
            this.props.onAgentIdChange(isValidId(this.state.agentId) ? this.state.agentId : null);
        });
    }

    handleAgentUrlChange (agentUrl) {
        this.setState({ agentUrl }, () => {
            this.props.onAgentUrlChange(isValidUrlWithPortAndPath(this.state.agentUrl) ? this.state.agentUrl : null);
        });
    }

    handleServerUrlChange (serverUrl) {
        this.setState({ serverUrl }, () => {
            this.props.onServerUrlChange(isValidUrlWithPort(this.state.serverUrl) ? this.state.serverUrl : null);
        });
    }

    render () {
        const isAgentUrlValid = isValidUrlWithPortAndPath(this.state.agentUrl);
        const isServerUrlValid = isValidUrlWithPort(this.state.serverUrl);
        const isAgentIdValid = isValidId(this.state.agentId);

        return (
            <Form horizontal>
                <FormGroupInput
                    isValid={ isAgentIdValid }
                    label={ t("console.applications.agents.common.agents.new.agentId.title") }
                    onChange={ this.handleAgentIdChange }
                    validationMessage={ t("console.common.validation.invalidCharacters") }
                    value={ this.state.agentId }
                />
                <FormGroupInput
                    isValid={ isAgentUrlValid }
                    label={ t("console.applications.agents.common.agentUrl.title") }
                    onChange={ this.handleAgentUrlChange }
                    placeholder={ t("console.applications.agents.java.agents.new.agentUrl.placeholder") }
                    validationMessage={ t("console.applications.agents.java.agents.new.agentUrl.validationMessage") }
                    value={ this.state.agentUrl }
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

NewJavaAgentForm.propTypes = {
    children: PropTypes.node.isRequired,
    onAgentIdChange: PropTypes.func.isRequired,
    onAgentUrlChange: PropTypes.func.isRequired,
    onServerUrlChange: PropTypes.func.isRequired
};

export default NewJavaAgentForm;
