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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Form, Button, Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";
import { cloneDeep } from "lodash";
import { t } from "i18next";
import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import "codemirror/mode/javascript/javascript";
import "codemirror/addon/display/placeholder";
import CodeMirror from "codemirror/lib/codemirror";
import {
    categorySchema,
    splitCategoryAndTags
} from "org/forgerock/openam/ui/admin/utils/nodeDesignerUtils";

let scriptEditor;
let propertiesEditor;
const SCRIPT_EDITOR_CONFIG = {
    lineNumbers: true,
    autofocus: false,
    viewportMargin: Infinity,
    mode: "javascript",
    theme: "forgerock"
};

class EditNode extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
        }
    }

    componentDidUpdate () {
        if (this.props.data && this.props.schema) {
            const data = cloneDeep(this.props.data);
            const schema = cloneDeep(this.props.schema);
            schema.properties.category = categorySchema;

            // The backend doesn't have a category property. In order to get
            // the value of category we need to pull them from the tags property
            const tagsAndCategory = splitCategoryAndTags(data.tags);
            data.tags = tagsAndCategory.tags;
            data.category = tagsAndCategory.category;
            // Use json stringify to pretty print the value of the properties
            // field
            data.properties = JSON.stringify(data.properties, null, 2);

            if (this.jsonSchemaView) {
                this.jsonForm.removeChild(this.jsonSchemaView.render().el);
                this.jsonSchemaView = new FlatJSONSchemaView({
                    schema: new JSONSchema(schema),
                    values: new JSONValues(data)
                });
                this.jsonForm.appendChild(this.jsonSchemaView.render().el);
            } else {
                this.jsonSchemaView = new FlatJSONSchemaView({
                    schema: new JSONSchema(schema),
                    values: new JSONValues(data)
                });
                this.jsonForm.appendChild(this.jsonSchemaView.render().el);
            }

            // Convert the script and properties fields generated by JSONSchemaView
            // to use codeMirror script editor fields instead
            const script = document.querySelector('[name="root[script]"]');
            scriptEditor = CodeMirror.fromTextArea(script, SCRIPT_EDITOR_CONFIG);

            const properties = document.querySelector('[name="root[properties]"]');
            propertiesEditor = CodeMirror.fromTextArea(properties, SCRIPT_EDITOR_CONFIG);
            // Set the placeholder value for properties field
            propertiesEditor.setOption("placeholder", this.props.schema.properties.properties.exampleValue);
            // style the properties field allow for the length of the placeholder
            propertiesEditor.getWrapperElement().classList.add("properties-field");
        }
    }

    setRef = (element) => {
        this.jsonForm = element;
    };

    handleSave = () => {
        const data = cloneDeep(this.jsonSchemaView.getData());

        // As the script and properties fields have been converted to codeMirror
        // script editors, jsonSchemaView no longer contains their value, instead
        // get their values from the scriptEditors themselves before saving
        data.script = scriptEditor.getValue();
        data.properties = propertiesEditor.getValue();
        // The backend doesn't have a category property. Instead we have to
        // combine category and tags in to the tags property
        if (data.category !== "") {
            data.tags = [...data.tags, data.category];
        }

        this.props.onSave(data);
    };

    render () {
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
                    icon="pencil-square"
                    subtitle={ t("console.authentication.nodes.edit.type") }
                    title={ this.props.id }
                >
                    <Button onClick={ this.props.onDelete }>
                        <i className="fa fa-times" /> { t("common.form.delete") }
                    </Button>
                </PageHeader>
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <EditFooter onSaveClick={ this.handleSave } />
                    </Panel.Footer>
                </Panel>
            </div>
        );
    }
}

EditNode.propTypes = {
    data: PropTypes.objectOf(PropTypes.object).isRequired,
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onDelete: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object).isRequired,
    values: PropTypes.objectOf(PropTypes.object).isRequired
};

export default EditNode;
