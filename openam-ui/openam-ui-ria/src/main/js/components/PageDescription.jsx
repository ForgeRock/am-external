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
 * A page description.
 * @module components/PageDescription
 * @param {Object} props Properties passed to this component
 * @param {ReactNode} props.children Children to add within this component
 * @returns {ReactElement} Renderable React element
 */
const PageDescription = ({ children }) => <p className="page-description">{ children }</p>;

PageDescription.propTypes = {
    children: PropTypes.node
};

export default PageDescription;
