/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
