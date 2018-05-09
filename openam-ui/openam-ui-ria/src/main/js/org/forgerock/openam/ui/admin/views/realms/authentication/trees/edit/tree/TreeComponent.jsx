/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import Measure from "react-measure";
import PropTypes from "prop-types";
import React from "react";
import classNames from "classnames";

const TreeComponent = ({
    children,
    className,
    id,
    isSelected,
    node,
    onMeasure,
    onMouseDown,
    onMouseUp,
    titleDecoration
}) => {
    const handleMeasure = (dimensions) => {
        if (onMeasure) {
            onMeasure(id, dimensions);
        }
    };
    return (
        <div
            onMouseDown={ onMouseDown }
            role="presentation"
        >
            <Measure onMeasure={ handleMeasure }>
                <div
                    className={ classNames(className, {
                        "authtree-node": true,
                        "authtree-node-selected": isSelected
                    }) }
                    onMouseUp={ onMouseUp }
                    role="presentation"
                >
                    { titleDecoration }
                    <div className="authtree-node-title">{ node.displayName }</div>
                    { children }
                </div>
            </Measure>
        </div>
    );
};

TreeComponent.defaultProps = {
    className: ""
};

TreeComponent.propTypes = {
    children: PropTypes.node,
    className: PropTypes.PropTypes.string,
    id: PropTypes.string.isRequired,
    isSelected: PropTypes.bool.isRequired,
    node: PropTypes.shape({
        displayName: PropTypes.string.isRequired
    }).isRequired,
    onMeasure: PropTypes.func,
    onMouseDown: PropTypes.func.isRequired,
    onMouseUp: PropTypes.func,
    titleDecoration: PropTypes.node
};

export default TreeComponent;
