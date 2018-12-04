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

import EnumArrayInput from "components/form/inputs/array/EnumArrayInput";

const EnumArrayField = ({ autofocus, formData, ...restProps }) => (
    <EnumArrayInput
        /**
         * Attribute remapping of autofocus required due to following react-select change
         * @see https://github.com/JedWatson/react-select/pull/2002
         */
        autoFocus={ autofocus } // eslint-disable-line jsx-a11y/no-autofocus
        value={ formData }
        { ...restProps }
    />
);

EnumArrayField.propTypes = {
    autofocus: PropTypes.bool,
    formData: PropTypes.arrayOf(PropTypes.any)
};

export default EnumArrayField;
