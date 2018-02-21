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

import { Media } from "react-bootstrap";
import React, { PropTypes } from "react";

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
