/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { ControlLabel, Form, FormControl, FormGroup } from "react-bootstrap";
import { t } from "i18next";
import { isEmpty, uniqueId } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const EditTreeNodeDisplayName = ({ nodeId, nodeName, onDisplayNameChange }) => {
    const handleDisplayNameChange = (event) => onDisplayNameChange(nodeId, event.target.value);
    const isValid = !isEmpty(nodeName);
    return (
        <Form>
            <FormGroup controlId={ uniqueId("nodeDisplayName") } validationState={ isValid ? null : "error" }>
                <ControlLabel>{ t("console.authentication.trees.edit.nodes.selectedNode.nodeName") }</ControlLabel>
                <FormControl
                    onBlur={ handleDisplayNameChange }
                    onChange={ handleDisplayNameChange }
                    type="text"
                    value={ nodeName }
                />
            </FormGroup>
        </Form>
    );
};

EditTreeNodeDisplayName.propTypes = {
    nodeId: PropTypes.string,
    nodeName: PropTypes.string,
    onDisplayNameChange: PropTypes.func.isRequired
};

export default EditTreeNodeDisplayName;
