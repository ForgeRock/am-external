/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import classNames from "classnames";
import PropTypes from "prop-types";
import React from "react";

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
