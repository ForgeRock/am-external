/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { uniqueId } from "lodash";
import classNames from "classnames";
import PropTypes from "prop-types";
import React from "react";

const NodeOutcome = ({ id, isConnected, name, onMouseDown, onMouseUp }) => {
    const uniqueOutcomeId = uniqueId(`outcome-${id}`);
    const handleMouseDown = (event) => onMouseDown(event, id);

    return (
        <div>
            <label className="sr-only" htmlFor={ uniqueOutcomeId }>{ name }</label>
            <div
                className={ classNames({
                    "authtree-outcome": true,
                    "authtree-outcome-invalid": !isConnected
                }) }
                id={ uniqueOutcomeId }
                onMouseDown={ handleMouseDown }
                onMouseUp={ onMouseUp }
                role="presentation"
            />
        </div>
    );
};

NodeOutcome.propTypes = {
    id: PropTypes.string.isRequired,
    isConnected: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    onMouseDown: PropTypes.func.isRequired,
    onMouseUp: PropTypes.func.isRequired
};

export default NodeOutcome;