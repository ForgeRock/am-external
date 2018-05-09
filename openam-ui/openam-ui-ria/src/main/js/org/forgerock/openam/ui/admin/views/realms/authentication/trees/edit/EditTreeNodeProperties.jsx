/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Alert } from "react-bootstrap";
import { get, has } from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

import Form from "components/form/Form";
import EditTreeNodeDisplayName from "./EditTreeNodeDisplayName";

const EditTreeNodeProperties = ({ isExpanded, isNew, nodeId, nodeName, nodeType, onDisplayNameChange, onFieldChange,
    onPropertiesChange, properties, schema }) => {
    const handleChange = ({ formData }) => {
        onFieldChange(formData);
        onPropertiesChange(nodeId, nodeType, formData);
    };
    const handleSubmit = () => onPropertiesChange(nodeId, nodeType, properties);

    let title;
    let displayName;
    let content;

    const nodeTypeName = get(properties, "_type.name");

    if (isExpanded && nodeTypeName) {
        title = <h4 className="authtree-content-side-title text-primary">{ nodeTypeName }</h4>;
        displayName = (
            <EditTreeNodeDisplayName
                nodeId={ nodeId }
                nodeName={ nodeName }
                onDisplayNameChange={ onDisplayNameChange }
            />
        );
        content = has(schema, "properties")
            ? (
                <span>
                    <hr />
                    <Form
                        formData={ properties }
                        onChange={ handleChange }
                        onSubmit={ handleSubmit }
                        schema={ schema }
                        validationMode={ isNew ? "create" : "edit" }
                    >
                        <button className="hidden" type="submit" />
                    </Form>
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
                "expanded": !!isExpanded
            }) }
        >
            { title }
            { displayName }
            { content }
        </div>
    );
};

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
