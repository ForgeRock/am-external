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
import classNames from "classnames";
import React, { PropTypes } from "react";

const MIN_CONTROL_POINT_LENGTH = 50;
const MAX_VERTICAL_DISTANCE = 150;

const bezierCurve = ({ start, startCp, center, endCp, end }) => {
    const centerControlPoints = center ? `${center.x} ${center.y} , ${center.x} ${center.y} S` : "";
    const startControlPoints = `M${start.x} ${start.y} C${startCp.x} ${startCp.y}, `;
    const endControlPoints = `${endCp.x} ${endCp.y}, ${end.x} ${end.y}`;

    return `${startControlPoints}${centerControlPoints}${endControlPoints}`;
};

const calculateOffsetX = (start, end) => {
    const strength = 0.2;
    let minXLength = MIN_CONTROL_POINT_LENGTH;
    if (Math.abs(start.y - end.y) < MIN_CONTROL_POINT_LENGTH) {
        // reduce the minXLength if the nodes are very close together
        minXLength = Math.abs(start.y - end.y);
    }
    const offSetX = Math.abs((start.x - end.x) * strength);

    return offSetX < minXLength ? minXLength : offSetX;
};

const createFourPointBezierCurve = (start, end) => {
    const clearance = (start.height + start.width) / 2 || 0;
    const offsetX = calculateOffsetX(start, end);
    const startCp = {
        x: start.x + offsetX,
        y: start.y + clearance
    };
    const endCp = {
        x: end.x - offsetX,
        y: end.y + MIN_CONTROL_POINT_LENGTH
    };

    return bezierCurve({ start, startCp, endCp, end });
};

const createSixPointBezierCurve = (start, end) => {
    const offsetX = calculateOffsetX(start, end);
    const center = {
        x:start.x - (start.x - end.x) / 2,
        y:start.y - (start.y - end.y) / 2
    };
    const startCp = {
        x: start.x + offsetX,
        y: start.y
    };
    const endCp = {
        x: end.x - offsetX,
        y: end.y
    };

    return bezierCurve({ start, startCp, center, endCp, end });
};

const Connection = ({ end, isNew, start, isInputForSelectedNode, isOutputForSelectedNode }) => {
    const isWithinVerticalRange = Math.abs(start.y - end.y) < MAX_VERTICAL_DISTANCE;
    const isEndNodeBehindStartNode = end.x + start.width <= start.x;
    const pathString = isEndNodeBehindStartNode && isWithinVerticalRange
        ? createFourPointBezierCurve(start, end) : createSixPointBezierCurve(start, end);

    return (
        <path
            className={ classNames({
                "authtree-connector": true,
                "authtree-connector-new": isNew,
                "authtree-connector-selected-node-input": isInputForSelectedNode,
                "authtree-connector-selected-node-output": isOutputForSelectedNode
            }) }
            d={ pathString }
            id={ end.id ? `connection-${start.id}-${end.id}` : `connection-${start.id}` }
        />
    );
};

Connection.propTypes = {
    end: PropTypes.shape({
        /**
         * end id not required as this connection might be being made (being dragged out),
         * where there is a start node but no end node yet.
         */
        id: PropTypes.string,
        height: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired
    }).isRequired,
    isInputForSelectedNode: PropTypes.bool.isRequired,
    isNew: PropTypes.bool.isRequired,
    isOutputForSelectedNode: PropTypes.bool.isRequired,
    start: PropTypes.shape({
        id: PropTypes.string.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired
    }).isRequired
};

export default Connection;
