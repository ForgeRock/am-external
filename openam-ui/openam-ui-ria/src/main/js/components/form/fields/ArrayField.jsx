/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Creatable } from "react-select";
import { map, pluck } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const ArrayField = ({ formData, onChange, ...restProps }) => {
    const handleOnChange = (value) => onChange(pluck(value, "value"));
    const handleShouldKeyDownEventCreateNewOption = ({ keyCode }) => {
        switch (keyCode) {
            case 9: // TAB
            case 13: // ENTER
                return true;
        }
        return false;
    };

    return (
        <Creatable
            { ...restProps }
            multi
            onChange={ handleOnChange }
            options={ map(formData, (data) => ({ label: data, value: data })) }
            shouldKeyDownEventCreateNewOption={ handleShouldKeyDownEventCreateNewOption }
            value={ map(formData, (data) => ({ label: data, value: data })) }
        />
    );
};

ArrayField.propTypes = {
    formData: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func.isRequired
};

export default ArrayField;
