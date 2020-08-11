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

import React, { Fragment } from "react";
import { ControlLabel } from "react-bootstrap";

import SingleSelect from "components/inputs/select/SingleSelect";

const mapEnumNames = (items) => {
    const hasEnumNames = items.enumNames;
    return items.enum.map((label, index) => {
        return { label, value: hasEnumNames ? items.enumNames[index] : label };
    });
};
/**
* Given an onUpdate function, a default value and a jsonschema property, this module will return a SingleSelect
* component.
* @param {Object} onUpdate The update function
* @param {Object} props Component props
* @param {Object} props.defaultValue The initial value for the component
* @param {Object} props.schema The jsonschema property
* @returns {Object} a function containing the SingleSelect component.
 */
const singleSelectEditor = (onUpdate, { defaultValue, schema }) => {
    function handleUpdate ({ value }, { action }) {
        if (action === "select-option") {
            onUpdate(value);
        }
    }

    const options = mapEnumNames(schema);
    const value = options.filter(({ label }) => label === defaultValue)[0];

    return (
        <Fragment>
            <ControlLabel htmlFor={ schema._id } srOnly>{ schema.title }</ControlLabel>
            <SingleSelect
                inputId={ schema._id }
                onChange={ handleUpdate }
                options={ options }
                value={ value }
            />
        </Fragment>
    );
};

export default singleSelectEditor;
