/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

const ToggleSwitch = (props) => (
    <label className="am-toggle-switch" htmlFor={ props.id }>
        <input { ...props } type="checkbox" />
        <span />
    </label>
);

ToggleSwitch.propTypes = {
    checked: PropTypes.bool.isRequired,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
};

export default ToggleSwitch;
