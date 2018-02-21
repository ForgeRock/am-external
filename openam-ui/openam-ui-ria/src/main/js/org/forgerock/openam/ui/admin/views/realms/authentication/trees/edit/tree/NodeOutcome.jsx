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
import { uniqueId } from "lodash";
import React, { PropTypes } from "react";
import classNames from "classnames";

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
