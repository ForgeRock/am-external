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
 * Copyright 2017-2026 Ping Identity Corporation.
 */

import classNames from "classnames";
import Measure from "react-measure";
import PropTypes from "prop-types";
import React from "react";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import { t } from "i18next";

export default function TreeComponent (props) {
    function handleMeasure (dimensions) {
        if (props.onMeasure) {
            props.onMeasure(props.id, dimensions);
        }
    }

    const upgradeIcon = (
        props.node.isUpgradable &&
        <OverlayTrigger
            overlay={
                <Tooltip id={ `${props.id}-tooltip` }>
                    { t("console.authentication.trees.edit.nodes.nodeTypes.updateAvailable") }
                </Tooltip>
            }
            placement="top"
        >
            <div className="node-upgrade-icon">
                <i className="fa fa-refresh" />
            </div>
        </OverlayTrigger>
    );

    return (
        <div
            onMouseDown={ props.onMouseDown }
            role="presentation"
        >
            <Measure onMeasure={ handleMeasure }>
                <div
                    className={ classNames(props.className, {
                        "authtree-node": true,
                        "authtree-node-selected": props.isSelected
                    }) }
                    onMouseUp={ props.onMouseUp }
                    role="presentation"
                >
                    { upgradeIcon }
                    { props.titleDecoration }
                    <div className="authtree-node-title">
                        { props.node.displayName }
                    </div>
                    { props.children }
                </div>
            </Measure>
        </div>
    );
}

TreeComponent.defaultProps = {
    className: ""
};

TreeComponent.propTypes = {
    children: PropTypes.node,
    className: PropTypes.PropTypes.string,
    id: PropTypes.string.isRequired,
    isSelected: PropTypes.bool.isRequired,
    node: PropTypes.shape({
        displayName: PropTypes.string.isRequired,
        isUpgradable: PropTypes.bool
    }).isRequired,
    onMeasure: PropTypes.func,
    onMouseDown: PropTypes.func.isRequired,
    onMouseUp: PropTypes.func,
    titleDecoration: PropTypes.node
};
