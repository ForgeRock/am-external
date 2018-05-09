/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Col, ControlLabel, FormControl, FormGroup, HelpBlock } from "react-bootstrap";
import { uniqueId } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const FormGroupInput = ({ isValid, value, validationMessage, label, onChange, placeholder }) => {
    const handleOnChange = (event) => onChange(event.target.value);
    const message = !isValid && validationMessage
        ? <HelpBlock><small dangerouslySetInnerHTML={ { __html: validationMessage } } /></HelpBlock> // eslint-disable-line react/no-danger
        : null;

    return (
        <FormGroup controlId={ uniqueId("formGroupInput") } validationState={ isValid ? null : "error" }>
            <Col componentClass={ ControlLabel } sm={ 4 }>{ label }</Col>
            <Col sm={ 6 }>
                <FormControl
                    onChange={ handleOnChange }
                    placeholder={ placeholder }
                    type="text"
                    value={ value }
                />
                { message }
            </Col>
        </FormGroup>
    );
};

FormGroupInput.defaultProps = {
    isValid: true
};

FormGroupInput.propTypes = {
    isValid: PropTypes.bool,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    validationMessage: PropTypes.string,
    value: PropTypes.string.isRequired
};

export default FormGroupInput;
