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
 * Copyright 2024-2025 Ping Identity Corporation.
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
import { EditorState } from "@codemirror/state";
import { EditorView, keymap, lineNumbers, placeholder } from "@codemirror/view";
import { javascript } from "@codemirror/lang-javascript";
import { defaultKeymap } from "@codemirror/commands";
import { forgerockCmTheme } from "org/forgerock/openam/ui/admin/utils/cm-theme";
import {
    categorySchema,
    splitCategoryAndTags
} from "org/forgerock/openam/ui/admin/utils/nodeDesignerUtils";

class EditNode extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
        }
    }

    componentDidUpdate () {
        if (this.props.data && this.props.schema) {
            if (this.scriptEditor) {
                this.scriptEditor.destroy();
            }
            if (this.propertiesEditor) {
                this.propertiesEditor.destroy();
            }

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

            const scriptTextArea = document.querySelector('[name="root[script]"]');
            if (scriptTextArea) {
                const parent = scriptTextArea.parentElement;
                scriptTextArea.style.display = "none";
                const editorDiv = document.createElement("div");
                editorDiv.classList.add("codemirror-field");
                parent.appendChild(editorDiv);

                this.scriptEditor = new EditorView({
                    state: EditorState.create({
                        doc: scriptTextArea.value,
                        extensions: [
                            ...forgerockCmTheme,
                            lineNumbers(),
                            javascript(),
                            keymap.of(defaultKeymap)
                        ]
                    }),
                    parent: editorDiv
                });
            }

            const propertiesTextArea = document.querySelector('[name="root[properties]"]');
            if (propertiesTextArea) {
                const parent = propertiesTextArea.parentElement;
                propertiesTextArea.style.display = "none";
                const editorDiv = document.createElement("div");
                editorDiv.classList.add("codemirror-field");
                parent.appendChild(editorDiv);

                this.propertiesEditor = new EditorView({
                    state: EditorState.create({
                        doc: propertiesTextArea.value,
                        extensions: [
                            ...forgerockCmTheme,
                            lineNumbers(),
                            javascript(),
                            keymap.of(defaultKeymap),
                            placeholder(this.props.schema.properties.properties.exampleValue)
                        ]
                    }),
                    parent: editorDiv
                });
            }
        }
    }

    componentWillUnmount () {
        if (this.scriptEditor) {
            this.scriptEditor.destroy();
        }
        if (this.propertiesEditor) {
            this.propertiesEditor.destroy();
        }
    }

    scriptEditor = null;
    propertiesEditor = null;

    setRef = (element) => {
        this.jsonForm = element;
    };

    handleSave = () => {
        const data = cloneDeep(this.jsonSchemaView.getData());

        // As the script and properties fields have been converted to codeMirror
        // script editors, jsonSchemaView no longer contains their value, instead
        // get their values from the scriptEditors themselves before saving
        if (this.scriptEditor) {
            data.script = this.scriptEditor.state.doc.toString();
        }
        if (this.propertiesEditor) {
            data.properties = this.propertiesEditor.state.doc.toString();
        }
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
