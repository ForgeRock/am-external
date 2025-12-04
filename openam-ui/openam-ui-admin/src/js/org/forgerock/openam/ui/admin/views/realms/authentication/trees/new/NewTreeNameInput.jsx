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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

import { Col, ControlLabel, FormControl, Form, FormGroup } from "react-bootstrap";
import { t } from "i18next";
import { uniqueId } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

class NewTreeNameInput extends Component {
    handleSubmit = (event) => event.preventDefault();
    handleTreeNameChange = (event) => this.props.onTreeNameChange(event.target.value.trimStart());

    render () {
        return (
            <Form horizontal onSubmit={ this.handleSubmit }>
                <FormGroup controlId={ uniqueId("treeName") }>
                    <ControlLabel className="col-sm-4">
                        { t("console.authentication.trees.new.treeName") }
                    </ControlLabel>
                    <Col sm={ 6 }>
                        <FormControl onChange={ this.handleTreeNameChange } type="text" value={ this.props.treeName } />
                    </Col>
                </FormGroup>
            </Form>
        );
    }
}

NewTreeNameInput.propTypes = {
    onTreeNameChange: PropTypes.func.isRequired,
    treeName: PropTypes.string.isRequired
};

export default NewTreeNameInput;
