/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Col, ControlLabel, FormControl, Form, FormGroup } from "react-bootstrap";
import { t } from "i18next";
import { uniqueId } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const NewTreeNameInput = ({ treeName, onTreeNameChange }) => {
    const handleTreeNameChange = (event) => onTreeNameChange(event.target.value);

    return (
        <Form horizontal>
            <FormGroup controlId={ uniqueId("treeName") }>
                <ControlLabel className="col-sm-4">
                    { t("console.authentication.trees.new.treeName") }
                </ControlLabel>
                <Col sm={ 6 }>
                    <FormControl onChange={ handleTreeNameChange } type="text" value={ treeName } />
                </Col>
            </FormGroup>
        </Form>
    );
};

NewTreeNameInput.propTypes = {
    onTreeNameChange: PropTypes.func.isRequired,
    treeName: PropTypes.string.isRequired
};

export default NewTreeNameInput;
