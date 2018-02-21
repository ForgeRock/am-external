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
 * Copyright 2017 ForgeRock AS.
 */

import { Alert } from "react-bootstrap";
import { has } from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import React, { PropTypes } from "react";

import Form from "components/form/Form";

const EditTreeNodeProperties = ({ isExpanded, nodeId, nodeName, nodeType, onFieldChange, onPropertiesChange, properties,
    schema }) => {
    const handleOnFieldChange = ({ formData }) => onFieldChange(formData);
    const handlePropertiesChange = () => onPropertiesChange(nodeId, nodeType, properties);

    let title;
    let content;

    if (isExpanded) {
        title = <h4 className="authtree-content-side-title text-primary">{ nodeName }</h4>;
        content = has(schema, "properties")
            ? (
                <Form
                    formData={ properties }
                    onBlur={ handlePropertiesChange }
                    onChange={ handleOnFieldChange }
                    onSubmit={ handlePropertiesChange }
                    schema={ schema }
                >
                    <button className="hidden" type="submit" />
                </Form>
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
            { content }
        </div>
    );
};

EditTreeNodeProperties.propTypes = {
    isExpanded: PropTypes.bool.isRequired,
    nodeId: PropTypes.string,
    nodeName: PropTypes.string,
    nodeType: PropTypes.string,
    onFieldChange: PropTypes.func.isRequired,
    onPropertiesChange: PropTypes.func.isRequired,
    properties: PropTypes.objectOf(PropTypes.any),
    schema: PropTypes.objectOf(PropTypes.any)
};

export default EditTreeNodeProperties;
