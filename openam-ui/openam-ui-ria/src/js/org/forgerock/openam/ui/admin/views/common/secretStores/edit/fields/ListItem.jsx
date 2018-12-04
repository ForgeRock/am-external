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
 * Copyright 2018 ForgeRock AS.
 */

import { Badge, Button, ListGroupItem } from "react-bootstrap";
import { SortableHandle } from "react-sortable-hoc";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";

const DragHandle = SortableHandle(({ isActiveAlias, isCursorActive, value }) => {
    const activeBadge = isActiveAlias
        ? <Badge pullRight>{ t("console.secretStores.edit.mappings.form.aliasField.active") }</Badge>
        : null;
    return (
        <div
            className={ classnames({
                "custom-aliases-field-handle-cursor": isCursorActive,
                "custom-aliases-field-handle": true
            }) }
        >
            { activeBadge }
            <div className="custom-aliases-field-value" >
                { value }
            </div>
        </div>
    );
});

class ListItem extends Component {
    constructor (props) {
        super(props);

        this.handleDelete = this.handleDelete.bind(this);
    }

    handleDelete () {
        this.props.onDelete(this.props.value);
    }

    render () {
        return (
            <ListGroupItem
                bsStyle={ this.props.isActiveAlias ? "success" : null }
                className={ classnames({
                    /**
                     * The transform transition is removed within css and re-added to the sorting class due to an
                     * unresolved issue within react-sortable-hoc
                     * @see https://github.com/clauderic/react-sortable-hoc/issues/334
                     **/
                    "custom-aliases-field-list-group-item" : true,
                    "custom-aliases-field-list-group-item-sorting" : this.props.isListGroupSorting
                }) }
            >
                <Button
                    bsStyle="link"
                    className="pull-right"
                    onClick={ this.handleDelete }
                    style={ { margin: "-5px -15px 0 0" } }
                    title={ t("common.form.delete") }
                >
                    <i className="fa fa-close" />
                </Button>
                <DragHandle
                    isActiveAlias={ this.props.isActiveAlias }
                    isCursorActive={ this.props.isCursorActive }
                    value={ this.props.value }
                />
            </ListGroupItem>
        );
    }
}

ListItem.defaultPropTypes = {
    isCursorActive: true
};

ListItem.propTypes = {
    isActiveAlias: PropTypes.bool,
    isCursorActive: PropTypes.bool,
    isListGroupSorting: PropTypes.bool,
    onDelete: PropTypes.func.isRequired,
    value: PropTypes.string
};

export default ListItem;
