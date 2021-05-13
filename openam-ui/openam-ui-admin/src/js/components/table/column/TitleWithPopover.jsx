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
 * Copyright 2018-2019 ForgeRock AS.
 */
import { OverlayTrigger, Popover } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Fragment } from "react";

const TitleWithPopover = ({ popover, title }) => {
    if (popover) {
        const overly = (
            <Popover id="helpText" >
                <div
                    dangerouslySetInnerHTML={ { __html: popover } } // eslint-disable-line react/no-danger
                />
            </Popover>
        );

        return (
            <Fragment>
                { title }
                { " " }
                <OverlayTrigger overlay={ overly } placement="top">
                    <i className="fa fa-info-circle text-primary" />
                </OverlayTrigger>
            </Fragment>
        );
    } else {
        return title;
    }
};

TitleWithPopover.propTypes = {
    popover: PropTypes.string,
    title: PropTypes.string.isRequired
};

export default TitleWithPopover;
