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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

import { getUiOptions } from "react-jsonschema-form/lib/utils";
import { isFunction } from "lodash";
import { Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

class PanelCollapse extends Component {
    static propTypes = {
        children: PropTypes.node.isRequired,
        id: PropTypes.string,
        label: PropTypes.string,
        uiSchema: PropTypes.shape({
            "ui:options": PropTypes.shape({
                expanded: PropTypes.oneOfType([PropTypes.bool, PropTypes.func])
            })
        })
    };

    state = {
        expanded: getUiOptions(this.props.uiSchema).expanded !== undefined || true
    };

    componentDidMount = () => {
        this.handleExpanded();
    };

    componentDidUpdate (prevProps, prevState) {
        const { expanded } = getUiOptions(this.props.uiSchema);
        if (expanded) {
            if (this.state.expanded && prevState.expanded) {
                this.handleExpanded();
            } else {
                /**
                 * Use of #setState within a conditional statement allowed.
                 * @see https://reactjs.org/docs/react-component.html#componentdidupdate
                 **/
                this.setState({ expanded: true }); // eslint-disable-line react/no-did-update-set-state
            }
        }
    }

    handleToggle = () => {
        this.setState({ expanded:!this.state.expanded });
    };

    handleExpanded = () => {
        const { expanded } = getUiOptions(this.props.uiSchema);
        if (isFunction(expanded)) {
            expanded();
        }
    };

    render () {
        return (
            <Panel
                expanded={ this.state.expanded }
                id={ this.props.id }
                onToggle={ this.handleToggle }
            >
                <Panel.Heading>
                    <Panel.Title toggle>{ this.props.label }</Panel.Title>
                </Panel.Heading>
                <Panel.Collapse onEntered={ this.handleExpanded }>
                    <Panel.Body>{ this.props.children }</Panel.Body>
                </Panel.Collapse>
            </Panel>
        );
    }
}

export default PanelCollapse;
