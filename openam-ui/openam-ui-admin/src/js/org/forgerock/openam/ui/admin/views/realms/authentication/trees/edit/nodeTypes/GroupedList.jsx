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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Button, Collapse } from "react-bootstrap";
import { isEmpty, map, sortBy } from "lodash";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";

import NodeTypeItem from "./NodeTypeItem";

class GroupedList extends Component {
    static propTypes = {
        defaultIsOpen: PropTypes.bool.isRequired,
        group: PropTypes.arrayOf(PropTypes.shape({
            _id: PropTypes.string.isRequired,
            icon: PropTypes.string.isRequired,
            name: PropTypes.string.isRequired,
            tags: PropTypes.arrayOf(PropTypes.string)
        })).isRequired,
        groupKey: PropTypes.string.isRequired
    };

    state = { isOpen: this.props.defaultIsOpen };

    handleHeaderClick = (event) => {
        event.preventDefault();
        this.setState({ isOpen: !this.state.isOpen });
    };

    render () {
        const { group, groupKey } = this.props;

        const nodeTypeItems = map(sortBy(group, (node) => node.name.toUpperCase()), ({ _id, icon, name, tags }) => (
            <NodeTypeItem
                displayName={ name }
                icon={ icon }
                key={ _id }
                nodeType={ _id }
                tags={ tags }
            />
        ));

        return isEmpty(group)
            ? ""
            : (
                <div className="clearfix edit-tree-node-type-item-tag-group">
                    <Button
                        bsClass="link"
                        className="text-uppercase text-muted"
                        componentClass="h5"
                        onClick={ this.handleHeaderClick }
                    >
                        { t(`console.authentication.trees.edit.nodes.nodeTypes.groups.${groupKey}`) }
                        <i
                            className={
                                classnames({
                                    "fa": true,
                                    "fa-angle-down": this.state.isOpen,
                                    "fa-angle-up": !this.state.isOpen,
                                    "pull-right": true
                                })
                            }
                        />
                    </Button>

                    <Collapse in={ this.state.isOpen } >
                        <div>
                            { nodeTypeItems }
                        </div>
                    </Collapse>
                </div>
            );
    }
}

export default GroupedList;
