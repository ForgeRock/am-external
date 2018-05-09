/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Form } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
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
            this.props.onServerUrlChange(isValidUrlWithPortAndPath(this.state.serverUrl) ? this.state.serverUrl : null);
        });
    }

    render () {
        const isAgentUrlValid = isValidUrlWithPortAndPath(this.state.agentUrl);
        const isServerUrlValid = isValidUrlWithPortAndPath(this.state.serverUrl);
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
