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
 * A page header which displays the title, and combination of the optional elements,
 * buttons, icon and the page type.
 * @module components/PageHeader
 * @param {Object} props Properties passed to this component
 * @param {string} props.title Text to display for the header
 * @param {ReactNode}[props.children] Buttons to add within this header
 * @param {string} [props.type] Text to display for the instance type
 * @param {string} [props.icon] Icon to display in the header
 * @returns {ReactElement} Renderable React element
 */
const PageHeader = ({ children, icon, title, type }) => {
    const buttonGroupClassName = type ? "deep" : "shallow";

    const circleWithIcon = icon
        ? <span className="header-icon pull-left bg-primary"><i className={ `fa fa-${icon}` } /></span>
        : null;

    const pageType = type ? <h4 className="page-type">{ type }</h4> : null;

    return (
        <header className="page-header page-header-no-border clearfix">
            { circleWithIcon }
            <div className={ `button-group pull-right ${buttonGroupClassName}-page-header-button-group` } >
                { children }
            </div>
            <div className="pull-left">
                { pageType }
                <h1 className="wordwrap">{ title }</h1>
            </div>
        </header>
    );
};

PageHeader.propTypes = {
    children: PropTypes.node,
    icon: PropTypes.string,
    title: PropTypes.string.isRequired,
    type: PropTypes.string
};

export default PageHeader;
