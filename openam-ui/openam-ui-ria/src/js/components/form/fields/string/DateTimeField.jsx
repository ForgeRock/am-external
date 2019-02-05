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
import PropTypes from "prop-types";
import React from "react";

import DateTimeInput from "components/form/inputs/string/DateTimeInput";

const DateTimeField = ({ formData, onChange, ...restProps }) => (
    <DateTimeInput
        dateTime={ formData }
        onUpdate={ onChange }
        { ...restProps }
    />
);

DateTimeField.propTypes = {
    formData: PropTypes.string,
    onChange: PropTypes.func.isRequired
};

export default DateTimeField;