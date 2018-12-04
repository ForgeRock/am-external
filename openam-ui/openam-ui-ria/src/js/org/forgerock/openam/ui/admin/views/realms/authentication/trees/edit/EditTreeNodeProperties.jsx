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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { Alert } from "react-bootstrap";
import { get, has } from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";

import Form from "components/form/Form";
import EditTreeNodeDisplayName from "./EditTreeNodeDisplayName";

class EditTreeNodeProperties extends Component {
    constructor (props) {
        super(props);

        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange ({ formData }) {
        this.props.onFieldChange(formData);
        this.props.onPropertiesChange(this.props.nodeId, this.props.nodeType, formData);
    }

    handleSubmit () {
        this.props.onPropertiesChange(this.props.nodeId, this.props.nodeType, this.props.properties);
    }

    render () {
        let title;
        let displayName;
        let content;

        const nodeTypeName = get(this.props.properties, "_type.name");

        if (this.props.isExpanded && nodeTypeName) {
            title = <h4 className="authtree-content-side-title text-primary">{ nodeTypeName }</h4>;
            displayName = (
                <EditTreeNodeDisplayName
                    nodeId={ this.props.nodeId }
                    nodeName={ this.props.nodeName }
                    onDisplayNameChange={ this.props.onDisplayNameChange }
                />
            );
            content = has(this.props.schema, "properties")
                ? (
                    <span>
                        <hr />
                        <Form
                            alwaysValidate
                            formData={ this.props.properties }
                            onChange={ this.handleChange }
                            onSubmit={ this.handleSubmit }
                            schema={ this.props.schema }
                            validationMode={ this.props.isNew ? "create" : "edit" }
                        />
                    </span>
                )
                : (
                    <Alert bsStyle="info">
                        <p>{ t("console.authentication.trees.edit.nodes.properties.noProperties") }</p>
                    </Alert>
                );
        }

        return (
            <div
                className={ classnames({
                    "authtree-content-side": true,
                    "authtree-content-right": true,
                    "expanded": !!this.props.isExpanded
                }) }
            >
                { title }
                { displayName }
                { content }
            </div>
        );
    }
}

EditTreeNodeProperties.propTypes = {
    isExpanded: PropTypes.bool.isRequired,
    isNew: PropTypes.bool.isRequired,
    nodeId: PropTypes.string,
    nodeName: PropTypes.string,
    nodeType: PropTypes.string,
    onDisplayNameChange: PropTypes.func.isRequired,
    onFieldChange: PropTypes.func.isRequired,
    onPropertiesChange: PropTypes.func.isRequired,
    properties: PropTypes.objectOf(PropTypes.any),
    schema: PropTypes.objectOf(PropTypes.any)
};

export default EditTreeNodeProperties;
