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
 * A card.
 * @module components/Card
 * @param {Object} props Properties passed to this component
 * @param {string} props.href The link to the associated page
 * @param {ReactNode} props.children Children to add within this component
 * @param {string} props.icon Icon to display on the card
 * @returns {ReactElement} Renderable React element
 */
const Card = ({ href, children, icon }) => (
    <div className="panel-default panel am-panel-card" data-panel-card>
        <a href={ href }>
            <div className="card-body">
                <div className="card-icon-circle card-icon-circle-sm bg-primary">
                    <i className={ `fa ${icon}` } />
                </div>
                { children }
            </div>
        </a>
    </div>
);

Card.propTypes = {
    children: PropTypes.node,
    href: PropTypes.string.isRequired,
    icon: PropTypes.string.isRequired
};

export default Card;
