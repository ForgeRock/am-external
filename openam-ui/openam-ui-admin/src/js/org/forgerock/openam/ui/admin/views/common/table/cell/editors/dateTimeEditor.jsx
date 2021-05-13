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
 * Copyright 2018-2019 ForgeRock AS.
 */

import React from "react";
import { ControlLabel, FormGroup } from "react-bootstrap";

import DateTimeInput from "components/inputs/string/DateTimeInput";

/**
* Given a jsonschema property, this module will return a DateTimeInput component.
* @param {Object} onUpdate The update function
* @param {Object} props Component props
* @param {Object} props.defaultValue The initial value for the component
* @param {Object} props.schema The jsonschema property
* @returns {Object} a function containing the DateTimeInput component.
 */
const dateTimeEditor = (onUpdate, { defaultValue, schema }) => (
    <FormGroup
        controlId={ `dateTimeEditor-${schema._id}` }
        // We use the Formgroup controlId to pass down the Id to the FormControl which is inside of the
        // DateTimeInput. However we do not want to render the form-group styles, so here we override them
        style={ {
            marginBottom: 0
        } }

    >
        <ControlLabel srOnly>{ schema.title }</ControlLabel>
        <DateTimeInput
            // The autofocus is used here in the inline table component after the user has clicked inside of a
            // table cell. The dateTime is displayed in focus, the expected behaviour after a user interaction
            autoFocus // eslint-disable-line jsx-a11y/no-autofocus
            dateTime={ defaultValue }
            onUpdate={ onUpdate }
        />
    </FormGroup>
);

export default dateTimeEditor;
