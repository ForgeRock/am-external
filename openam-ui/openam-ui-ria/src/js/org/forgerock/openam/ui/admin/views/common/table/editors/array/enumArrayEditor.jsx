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
 * Copyright 2018 ForgeRock AS.
 */

import { ControlLabel } from "react-bootstrap";
import { isNumber } from "lodash";
import React, { Fragment } from "react";

import EnumArrayInput from "components/form/inputs/array/EnumArrayInput";

/**
 * Given an onUpdate function, a default value and a jsonschema property, this module will return a EnumArrayInput
 * component.
 * @param {Object} onUpdate The update function
 * @param {Object} props Component props
 * @param {Object} props.defaultValue The initial value for the component
 * @param {Object} props.schema The jsonschema property
 * @returns {Object} a function containing the EnumArrayInput component.
 */
const enumArrayEditor = (onUpdate, { defaultValue, schema }) => {
    function handleUpdate (value) {
        // TODO: Change this to use the JSON Schema validation engine
        const isValid = !schema.minItems || isNumber(schema.minItems) && value.length >= schema.minItems;
        if (isValid) {
            onUpdate(value);
        }
    }

    return (
        <Fragment>
            <ControlLabel htmlFor={ `enumArrayEditor-${schema._id}` } srOnly>
                { schema.title }
            </ControlLabel>
            <EnumArrayInput
                id={ `enumArrayEditor-${schema._id}` }
                onChange={ handleUpdate }
                schema={ schema }
                updateOnChange={ false }
                value={ defaultValue }
            />
        </Fragment>
    );
};

export default enumArrayEditor;
