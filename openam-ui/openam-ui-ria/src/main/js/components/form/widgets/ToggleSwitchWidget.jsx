/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

import ToggleSwitch from "components/ToggleSwitch";

const ToggleSwitchWidget = ({ onChange, id, value }) => {
    const handleOnChange = (event) => onChange(event.target.checked);

    return (
        <ToggleSwitch
            checked={ value }
            id={ id }
            onChange={ handleOnChange }
        />
    );
};

ToggleSwitchWidget.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.bool.isRequired
};

export default ToggleSwitchWidget;
