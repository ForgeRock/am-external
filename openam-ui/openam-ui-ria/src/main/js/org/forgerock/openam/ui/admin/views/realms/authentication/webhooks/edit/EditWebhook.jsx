/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Form, Button, Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { t } from "i18next";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import EditWebhookFooter from "./EditWebhookFooter";
import PageHeader from "components/PageHeader";

class EditWebhook extends Component {
    constructor (props) {
        super(props);

        this.setRef = this.setRef.bind(this);
        this.handleUpdate = this.handleUpdate.bind(this);
    }

    componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
        }
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.data && this.props.schema) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.data)
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    setRef (element) {
        this.jsonForm = element;
    }

    handleUpdate () {
        this.props.onSave(this.jsonSchemaView.getData());
    }

    render () {
        const footer = (
            <EditWebhookFooter
                onUpdateClick={ this.handleUpdate }
            />
        );

        const content = this.props.isFetching
            ? <Loading />
            : (
                <Form>
                    <div ref={ this.setRef } />
                </Form>
            );

        return (
            <div>
                <PageHeader
                    icon="anchor" title={ this.props.id }
                    type={ t("console.authentication.webhooks.edit.type") }
                >
                    <Button onClick={ this.props.onDelete }>
                        <i className="fa fa-times" /> { t("common.form.delete") }
                    </Button>
                </PageHeader>
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

EditWebhook.propTypes = {
    data: PropTypes.objectOf(PropTypes.object).isRequired,
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onDelete: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    values: PropTypes.objectOf(PropTypes.object).isRequired
};

export default EditWebhook;
