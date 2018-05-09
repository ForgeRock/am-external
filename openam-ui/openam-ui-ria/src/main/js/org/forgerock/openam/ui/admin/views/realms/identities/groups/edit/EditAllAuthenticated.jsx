/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Clearfix, Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";

class EditAllAuthenticated extends Component {
    constructor () {
        super();

        this.handleSave = this.handleSave.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
            this.jsonSchemaView.render();
        }
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.values && this.props.schema) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.values)
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    setRef (element) {
        this.jsonForm = element;
    }

    handleSave () {
        this.props.onSave(this.jsonSchemaView.getData());
    }

    render () {
        const footer = (
            <Clearfix>
                <div className="pull-right">
                    <div className="am-btn-action-group">
                        <Button
                            bsStyle="primary"
                            disabled={ this.props.isFetching }
                            onClick={ this.handleSave }
                        >
                            { t("common.form.saveChanges") }
                        </Button>
                    </div>
                </div>
            </Clearfix>
        );

        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <Form horizontal>
                    <div ref={ this.setRef } />
                </Form>
            );
        }

        return (
            <div>
                <PageHeader
                    icon="users"
                    title={ t("console.identities.groups.edit.allAuthenticated.title") }
                    type={ t("console.identities.groups.edit.allAuthenticated.type") }
                />
                <Panel footer={ footer }>
                    { content }
                </Panel>
            </div>
        );
    }
}

EditAllAuthenticated.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.shape({
        type: PropTypes.string
    }),
    values: PropTypes.shape({
        type: PropTypes.string
    })
};

export default EditAllAuthenticated;
