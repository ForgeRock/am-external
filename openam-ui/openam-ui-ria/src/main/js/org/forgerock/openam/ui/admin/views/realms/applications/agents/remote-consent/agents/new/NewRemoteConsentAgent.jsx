/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
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
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewRemoteConsentAgent extends Component {
    constructor (props) {
        super(props);
        this.state = {
            agentId: ""
        };
        this.handleAgentIdChange = this.handleAgentIdChange.bind(this);
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

    handleAgentIdChange (agentId) {
        this.setState({ agentId }, () => {
            this.props.onAgentIdChange(isValidId(this.state.agentId) ? this.state.agentId : null);
        });
    }

    setRef (element) {
        this.jsonForm = element;
    }

    handleCreate () {
        this.props.onCreate(this.jsonSchemaView.getData());
    }

    render () {
        const isAgentIdValid = isValidId(this.state.agentId);

        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsApplicationsAgentsRemoteConsent }
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
                        isValid={ isAgentIdValid }
                        label={ t("console.applications.agents.common.agents.new.agentId.title") }
                        onChange={ this.handleAgentIdChange }
                        validationMessage={ t("console.common.validation.invalidCharacters") }
                        value={ this.state.agentId }
                    />
                    <div ref={ this.setRef } />
                </Form>
            );
        }

        return (
            <div>
                <PageHeader title={ t("console.applications.agents.remoteConsent.agents.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewRemoteConsentAgent.propTypes = {
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onAgentIdChange: PropTypes.func.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired
};

export default NewRemoteConsentAgent;
