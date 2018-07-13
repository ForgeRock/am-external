/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016-2017 ForgeRock AS.
 */

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
    children: React.PropTypes.node,
    icon: React.PropTypes.string,
    title: React.PropTypes.string.isRequired,
    type: React.PropTypes.string
};

export default PageHeader;
