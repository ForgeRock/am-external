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

class NewWebAgentGroupForm extends Component {
    constructor (props) {
        super(props);
        this.state = {
            groupId: "",
            serverUrl: ""
        };
        this.handleGroupIdChange = this.handleGroupIdChange.bind(this);
        this.handleServerUrlChange = this.handleServerUrlChange.bind(this);
    }

    handleGroupIdChange (groupId) {
        this.setState({ groupId }, () => {
            this.props.onGroupIdChange(isValidId(this.state.groupId) ? this.state.groupId : null);
        });
    }

    handleServerUrlChange (serverUrl) {
        this.setState({ serverUrl }, () => {
            this.props.onServerUrlChange(isValidUrlWithPortAndPath(this.state.serverUrl) ? this.state.serverUrl : null);
        });
    }

    render () {
        const isServerUrlValid = isValidUrlWithPortAndPath(this.state.serverUrl);
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
