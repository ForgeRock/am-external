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
 * A block.
 * @module components/Block
 * @param {Object} props Properties passed to this component
 * @param {ReactNode[]} props.children Children to add within this component
 * @param {string} props.header Text to display for the block header
 * @param {string} [props.description] Text to display for the block description
 * @returns {ReactElement} Renderable React element
 */
const Block = ({ children, header, description }) => {
    const blockDescription = description ? <p className="block-description">{ description }</p> : undefined;

    return (
        <div className="block clearfix">
            <h3 className="block-header">{ header }</h3>
            { blockDescription }
            { children }
        </div>
    );
};

Block.propTypes = {
    children: PropTypes.arrayOf(PropTypes.node).isRequired,
    description: PropTypes.string,
    header: PropTypes.string.isRequired
};

export default Block;
