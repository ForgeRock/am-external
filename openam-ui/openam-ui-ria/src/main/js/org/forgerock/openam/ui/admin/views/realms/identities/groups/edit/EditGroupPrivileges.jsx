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

class EditGroupPrivileges extends Component {
    constructor (props) {
        super(props);

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
        if (!this.jsonSchemaView && this.props.values) {
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

EditGroupPrivileges.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object),
    values: PropTypes.objectOf(PropTypes.object)
};

export default EditGroupPrivileges;
