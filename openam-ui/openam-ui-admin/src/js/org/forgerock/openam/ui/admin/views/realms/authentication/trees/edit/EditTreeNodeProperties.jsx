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
 * Copyright 2017-2026 Ping Identity Corporation.
 */

import {
    Alert,
    Button,
    Dropdown,
    MenuItem
} from "react-bootstrap";
import {
    get,
    has
} from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { useMemo } from "react";

import EditTreeNodeDisplayName from "./EditTreeNodeDisplayName";
import ErrorBoundary from "components/ErrorBoundary";
import Form from "components/form/Form";
import { sortVersions } from "org/forgerock/openam/ui/admin/utils/nodeVersioningUtils";

const nodeTypeChangeHandlers = {
    ConfigProviderNode: (newProps, props) => {
        if (props.nodeType !== newProps.nodeType) {
            newProps.nodeVersion = null;
        }
    }
};

export default function EditTreeNodeProperties ({
    onUpdateNodeVersion,
    isExpanded,
    isNew,
    nodeId,
    nodeName,
    nodeType,
    onDisplayNameChange,
    onFieldChange,
    onPropertiesChange,
    properties,
    schema
}) {
    const sortedAvailableVersions = useMemo(() =>
        (properties && properties.versions ? sortVersions(properties.versions).reverse() : []),
    [properties]
    );

    function handleChange ({ formData }) {
        const handler = nodeTypeChangeHandlers[nodeType];
        if (handler) {
            handler(formData, properties);
        }
        onFieldChange(formData);
        onPropertiesChange(nodeId, nodeType, formData, properties.version);
    }

    function handleSubmit () {
        onPropertiesChange(nodeId, nodeType, properties, properties.version);
    }

    function updateNodeVersion (toVersion) {
        if (properties.version === toVersion) {
            return;
        }
        onUpdateNodeVersion(properties, toVersion);
    }

    let title;
    let displayName;
    let content;
    let versioningDropdown;
    let updateAvailableAlert;

    const nodeTypeName = get(properties, "_type.name");

    function generateMenuItem (version, index) { // eslint-disable-line
        const isCurrentVersion = version === properties.version;
        let textContent;
        let checkIcon;

        if (index === 0) {
            textContent = (
                <span>
                    { t("console.authentication.trees.edit.nodes.properties.latestVersion", { version }) }
                </span>
            );
        } else {
            textContent = (
                <span>v{ version }</span>
            );
        }

        if (isCurrentVersion) {
            checkIcon = (
                <span>
                    <i className="fa fa-check" />
                </span>
            );
        }

        return (
            <MenuItem
                active={ isCurrentVersion }
                key={ version }
                onClick={ () => updateNodeVersion(version) } // eslint-disable-line
            >
                { textContent }
                { checkIcon }
            </MenuItem>
        );
    }

    if (isExpanded && nodeTypeName) {
        title = <h4 className="authtree-content-side-title text-primary mb-0">{ nodeTypeName }</h4>;
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
                        editValidationMode={ !isNew }
                        formData={ properties }
                        liveValidate
                        onChange={ handleChange }
                        onSubmit={ handleSubmit }
                        schema={ schema }
                    />
                </span>
            )
            : (
                <Alert bsStyle="info">
                    <p>{ t("console.authentication.trees.edit.nodes.properties.noProperties") }</p>
                </Alert>
            );

        versioningDropdown = (
            <Dropdown
                bsStyle="link"
                className="node-version-dropdown"
            >
                <Dropdown.Toggle bsStyle="link">
                    v{ properties.version }
                    {
                        properties.isUpgradable &&
                        <div className="node-isupgradable-dot" />
                    }
                </Dropdown.Toggle>
                <Dropdown.Menu>
                    { sortedAvailableVersions.map((version, index) => generateMenuItem(version, index)) }
                </Dropdown.Menu>
            </Dropdown>
        );

        updateAvailableAlert = properties.isUpgradable && (
            <Alert
                bsStyle="danger"
                className="update-available-alert"
            >
                <h5>{ t("console.authentication.trees.edit.nodes.properties.updateAvailable") }</h5>
                <p>{ t("console.authentication.trees.edit.nodes.properties.updateAvailableDesc") }</p>
                <Button
                    bsStyle="link"
                    onClick={ () => updateNodeVersion(sortedAvailableVersions[0]) } // eslint-disable-line
                >
                    { t("console.authentication.trees.edit.nodes.properties.viewDetails") }
                </Button>
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
            <div className="node-heading">
                { title }
                { versioningDropdown }
                { updateAvailableAlert }
            </div>
            { displayName }
            <ErrorBoundary>
                { content }
            </ErrorBoundary>
        </div>
    );
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
    onUpdateNodeVersion: PropTypes.func.isRequired,
    properties: PropTypes.objectOf(PropTypes.any),
    schema: PropTypes.objectOf(PropTypes.any)
};
