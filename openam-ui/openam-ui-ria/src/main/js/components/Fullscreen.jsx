/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

/**
 * Adds and removes the "fullscreen" className and style.
 * @module components/Fullscreen
 * @param {Boolean} props.isFullscreen Determines if the fullscreen class will be applied or not.
 * @param {Boolean} [props.className] Class names.
 * @returns {ReactElement} Renderable React element
 */
const Fullscreen = ({ className, isFullscreen, ...restProps }) => {
    return (
        <div
            { ...restProps }
            className={ classnames(className, {
                "fullscreen": isFullscreen
            }) }
        />
    );
};

Fullscreen.propTypes = {
    className: PropTypes.string,
    isFullscreen: PropTypes.bool.isRequired
};

export default Fullscreen;
