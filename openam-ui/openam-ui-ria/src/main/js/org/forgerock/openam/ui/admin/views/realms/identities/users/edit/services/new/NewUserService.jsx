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
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

class NewUserService extends Component {
    constructor () {
        super();

        this.handleCreate = this.handleCreate.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.schema && this.props.template) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.template)
            });

            if (this.jsonForm) {
                this.jsonForm.appendChild(this.jsonSchemaView.render().el);
            }
        }
    }

    handleCreate () {
        this.props.onCreate(this.jsonSchemaView.getData());
    }

    setRef (element) {
        this.jsonForm = element;
        if (this.jsonForm && this.jsonSchemaView) {
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    render () {
        const footer = (
            <Footer
                backRoute={ Router.configuration.routes.realmsIdentitiesUsersEdit }
                backRouteArgs={ [this.props.id] }
                isCreateAllowed
                onCreateClick={ this.handleCreate }
            />
        );
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = <div ref={ this.setRef } />;
        }

        return (
            <div>
                <PageHeader
                    title={ t("console.identities.users.edit.services.new.subtitle", { type: this.props.type }) }
                />
                <Panel footer={ footer }>
                    <Form horizontal>
                        { content }
                    </Form>
                </Panel>
            </div>
        );
    }
}

NewUserService.propTypes = {
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    template: PropTypes.objectOf(PropTypes.object).isRequired,
    type: PropTypes.string.isRequired
};

export default NewUserService;
