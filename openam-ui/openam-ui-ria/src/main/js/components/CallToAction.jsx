/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

/**
 * Call to action component.
 * @module components/CallToAction
 * @param {ReactNode[]} props.children Children to add within this component
 * @returns {ReactElement} Renderable React element
 */
const CallToAction = ({ children }) => (
    <div className="call-to-action-block">
        { children }
    </div>
);

CallToAction.propTypes = {
    children: PropTypes.node
};

export default CallToAction;
