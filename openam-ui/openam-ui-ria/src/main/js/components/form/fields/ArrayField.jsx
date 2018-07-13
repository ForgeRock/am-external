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
 * Copyright 2017 ForgeRock AS.
 */

import { Creatable } from "react-select";
import { map, pluck } from "lodash";
import React, { PropTypes } from "react";

const ArrayField = ({ formData, onChange, ...restProps }) => {
    const handleOnChange = (value) => onChange(pluck(value, "value"));

    return (
        <Creatable
            { ...restProps }
            multi
            onChange={ handleOnChange }
            options={ map(formData, (data) => ({ label: data, value: data })) }
            value={ map(formData, (data) => ({ label: data, value: data })) }
        />
    );
};

ArrayField.propTypes = {
    formData: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func.isRequired
};

export default ArrayField;
