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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { OverlayTrigger, Popover } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const PopoverCell = ({ cell, children, dataFormat, formatExtraData, row, rowIdx }) => {
    const applyDataFormat = (format) => {
        return format ? React.cloneElement(format, { cell, row, formatExtraData, rowIdx }) : cell;
    };

    const overly = (
        <Popover id="table-cell-popover">
            <span className="wordwrap">
                { applyDataFormat(dataFormat) }
            </span>
        </Popover>
    );

    return (
        <OverlayTrigger delayShow={ 500 } overlay={ overly } placement="top">
            <span>
                { applyDataFormat(children) }
            </span>
        </OverlayTrigger>
    );
};

PopoverCell.propTypes = {
    cell: PropTypes.oneOfType([
        PropTypes.array,
        PropTypes.object,
        PropTypes.string
    ]),
    children: PropTypes.node,
    dataFormat: PropTypes.oneOfType([
        PropTypes.node,
        PropTypes.string
    ]),
    formatExtraData: PropTypes.objectOf(
        PropTypes.any
    ),
    row: PropTypes.objectOf(
        PropTypes.any
    ),
    rowIdx: PropTypes.number
};

export default PopoverCell;
