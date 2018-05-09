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
 * Copyright 2017-2018 ForgeRock AS.
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
