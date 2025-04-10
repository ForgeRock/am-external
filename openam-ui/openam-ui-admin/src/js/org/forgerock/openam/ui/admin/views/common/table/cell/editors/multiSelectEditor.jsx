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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { ControlLabel } from "react-bootstrap";
import { map, isNumber } from "lodash";
import React, { Fragment } from "react";

import MultiSelect from "components/inputs/select/MultiSelect";

const mapEnumNames = (items) => {
    const hasEnumNames = items.enumNames;
    return items.enum.map((label, index) => {
        return { label, value: hasEnumNames ? items.enumNames[index] : label };
    });
};
/**
 * Given an onUpdate function, a default value and a jsonschema property, this module will return a MultiSelect
 * component.
 * @param {Object} onUpdate The update function
 * @param {Object} props Component props
 * @param {Object} props.defaultValue The initial value for the component
 * @param {Object} props.schema The jsonschema property
 * @returns {Object} a function containing the MultiSelect component.
 */
const multiSelectEditor = (onUpdate, { defaultValue, schema }) => {
    function handleSave () {
        const isValid = !schema.minItems || isNumber(schema.minItems) && this.value.length >= schema.minItems;
        if (isValid) {
            onUpdate(map(this.value, "label"));
        }
    }

    const options = mapEnumNames(schema.items);
    const value = map(defaultValue, (value) => ({ label: value, value }));

    return (
        <Fragment>
            <ControlLabel htmlFor={ schema._id } srOnly>
                { schema.title }
            </ControlLabel>
            <MultiSelect
                defaultValue={ value }
                inputId={ schema._id }
                isClearable={ false }
                onBlur={ handleSave }
                options={ options }
            />
        </Fragment>
    );
};

export default multiSelectEditor;
