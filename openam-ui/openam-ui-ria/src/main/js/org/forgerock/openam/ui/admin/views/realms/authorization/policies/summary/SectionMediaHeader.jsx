/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Media } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const SectionMediaHeader = ({ icon, onClick, title }) => {
    const onHeaderClick = () => { onClick(title); };
    return (
        <Media
            onClick={ onHeaderClick }
            role="link"
        >
            <Media.Left>
                <div className="bg-primary">
                    <i className={ `fa fa-${icon}` } />
                </div>
            </Media.Left>

            <Media.Body>
                <Media.Heading>{ title }</Media.Heading>
            </Media.Body>

            <Media.Right align="middle">
                <i className="fa fa-pencil" />
            </Media.Right>
        </Media>
    );
};

SectionMediaHeader.propTypes = {
    icon: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired
};

export default SectionMediaHeader;
