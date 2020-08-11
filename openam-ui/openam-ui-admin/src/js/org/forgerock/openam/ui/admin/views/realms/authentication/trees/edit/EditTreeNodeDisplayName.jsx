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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { ControlLabel, Form, FormControl, FormGroup } from "react-bootstrap";
import { t } from "i18next";
import { isEmpty, uniqueId } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

class EditTreeNodeDisplayName extends Component {
    handleDisplayNameChange = (event) => {
        this.props.onDisplayNameChange(this.props.nodeId, event.target.value);
    };

    render () {
        const isValid = !isEmpty(this.props.nodeName);
        return (
            <Form>
                <FormGroup controlId={ uniqueId("nodeDisplayName") } validationState={ isValid ? null : "error" }>
                    <ControlLabel>{ t("console.authentication.trees.edit.nodes.selectedNode.nodeName") }</ControlLabel>
                    <FormControl
                        onBlur={ this.handleDisplayNameChange }
                        onChange={ this.handleDisplayNameChange }
                        type="text"
                        value={ this.props.nodeName }
                    />
                </FormGroup>
            </Form>
        );
    }
}

EditTreeNodeDisplayName.propTypes = {
    nodeId: PropTypes.string,
    nodeName: PropTypes.string,
    onDisplayNameChange: PropTypes.func.isRequired
};

export default EditTreeNodeDisplayName;
