/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import Footer from "org/forgerock/openam/ui/admin/views/realms/common/new/Footer";
import GroupedJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import NewJavaAgentForm from "./NewJavaAgentForm";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewJavaAgent extends Component {
    constructor (props) {
        super(props);
        this.handleCreate = this.handleCreate.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.template) {
            this.jsonSchemaView = new GroupedJSONSchemaView({
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
                backRoute={ Router.configuration.routes.realmsApplicationsAgentsJava }
                isCreateAllowed={ this.props.isCreateAllowed }
                onCreateClick={ this.handleCreate }
            />
        );
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <NewJavaAgentForm
                    onAgentIdChange={ this.props.onAgentIdChange }
                    onAgentUrlChange={ this.props.onAgentUrlChange }
                    onServerUrlChange={ this.props.onServerUrlChange }
                >
                    <div ref={ this.setRef } />
                </NewJavaAgentForm>
            );
        }

        return (
            <div>
                <PageHeader title={ t("console.applications.agents.java.agents.new.title") } />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

NewJavaAgent.propTypes = {
    isCreateAllowed: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onAgentIdChange: PropTypes.func.isRequired,
    onAgentUrlChange: PropTypes.func.isRequired,
    onCreate: PropTypes.func.isRequired,
    onServerUrlChange: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired
};

export default NewJavaAgent;
