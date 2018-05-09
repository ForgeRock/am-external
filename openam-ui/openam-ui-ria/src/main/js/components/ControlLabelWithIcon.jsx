/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Clearfix, ControlLabel } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const ControlLabelWithIcon = ({ children, icon }) => {
    const displayStyle = icon ? "block" : "inline-block";
    const iconContent = icon
        ? (
            <span
                className="pull-right"
                style={ { // required z-index and position to place icon above selectize components
                    position: "relative",
                    "zIndex": "30"
                } }
            >
                { icon }
            </span>
        )
        : null;
    return (
        <Clearfix>
            { iconContent }
            <ControlLabel style={ { display: displayStyle } }>{ children }</ControlLabel>
        </Clearfix>
    );
};

ControlLabelWithIcon.propTypes = {
    children: PropTypes.string.isRequired,
    icon: PropTypes.node
};

export default ControlLabelWithIcon;
