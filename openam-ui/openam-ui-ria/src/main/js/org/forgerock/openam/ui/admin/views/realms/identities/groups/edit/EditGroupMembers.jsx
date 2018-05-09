/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Clearfix, Col, Form, FormGroup, Panel } from "react-bootstrap";

import { isEqual } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";

class EditGroupMembers extends Component {
    constructor (props) {
        super(props);

        this.handleSave = this.handleSave.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView && !isEqual(this.props.schema, nextProps.schema)) {
            this.jsonSchemaView.destroy();
            this.jsonSchemaView = null;
        }

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
                <div>
                    <Form className="edit-group-member-selection" horizontal>
                        <div ref={ this.setRef } />
                    </Form>

                    <FormGroup>
                        <Col sm={ 6 } smOffset={ 4 }>
                            <div className="am-btn-action-group">
                                <Button onClick={ this.props.onAddAll }>
                                    { t("common.form.addAll") }
                                </Button>
                                <Button onClick={ this.props.onRemoveAll }>
                                    { t("common.form.removeAll") }
                                </Button>
                            </div>
                        </Col>
                    </FormGroup>
                </div>
            );
        }

        return (
            <Panel className="fr-panel-tab" footer={ footer }>
                { content }
            </Panel>
        );
    }
}

EditGroupMembers.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onAddAll: PropTypes.func.isRequired,
    onRemoveAll: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object),
    values: PropTypes.objectOf(PropTypes.object)
};

export default EditGroupMembers;
