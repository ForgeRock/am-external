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
 * Copyright 2017 ForgeRock AS.
 */

import { OverlayTrigger, Tooltip } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const EditTreeToolbarTooltip = ({ children, placement, tooltip }) => {
    const overlay = <Tooltip>{ tooltip }</Tooltip>;

    return (
        <OverlayTrigger
            delayShow={ 500 }
            overlay={ overlay }
            placement={ placement }
            rootClose
        >
            { children }
        </OverlayTrigger>
    );
};

EditTreeToolbarTooltip.propTypes = {
    children: PropTypes.node,
    placement: PropTypes.oneOf(["bottom", "top"]).isRequired,
    tooltip: PropTypes.string.isRequired
};

export default EditTreeToolbarTooltip;