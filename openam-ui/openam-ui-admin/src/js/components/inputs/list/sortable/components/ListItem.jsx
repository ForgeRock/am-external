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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Button, ListGroupItem } from "react-bootstrap";
import { SortableHandle } from "react-sortable-hoc";
import { t } from "i18next";
import classnames from "classnames";
import PropTypes from "prop-types";
import React, { Component } from "react";
import ScrollIntoViewIfNeeded from "react-scroll-into-view-if-needed";

const DragHandle = SortableHandle(({ children, isCursorActive }) => {
    return (
        <div
            className={ classnames({
                "sortable-array-field-handle-cursor": isCursorActive,
                "sortable-array-field-handle": true
            }) }
        >
            <div className="sortable-array-field-value" >
                { children }
            </div>
        </div>
    );
});

class ListItem extends Component {
    static propTypes = {
        bsStyle: PropTypes.string,
        children: PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.node
        ]).isRequired,
        isCursorActive: PropTypes.bool,
        isDeleteable: PropTypes.bool,
        isLastTouched: PropTypes.bool,
        isListGroupSorting: PropTypes.bool,
        onDelete: PropTypes.func.isRequired,
        onEdit: PropTypes.func.isRequired,
        position: PropTypes.number.isRequired,
        scrollTo: PropTypes.bool
    };

    static defaultPropTypes = {
        isCursorActive: true,
        scrollTo: false
    };

    handleDelete = () => {
        this.props.onDelete(this.props.position);
    };

    handleEdit = () => {
        this.props.onEdit(this.props.position);
    };

    render () {
        const { bsStyle, children, isCursorActive, isDeleteable, isListGroupSorting, isLastTouched, scrollTo } =
            this.props;

        const buttons = (
            <>
                <Button
                    bsStyle="link"
                    disabled={ !isDeleteable }
                    onClick={ this.handleEdit }
                    style={ {
                        position: "absolute",
                        padding: "0 6px",
                        right: 30
                    } }
                    title={ t("common.form.edit") }
                >
                    <i className="fa fa-pencil" />
                </Button>
                <Button
                    bsStyle="link"
                    disabled={ !isDeleteable }
                    onClick={ this.handleDelete }
                    style={ {
                        position: "absolute",
                        padding: "0 6px",
                        right: 5
                    } }
                    title={ t("common.form.delete") }
                >
                    <i className="fa fa-times" />
                </Button>
            </>
        );

        return (
            <ListGroupItem
                bsStyle={ bsStyle }
                className={ classnames({
                    /**
                     * The transform transition is removed within css and re-added to the sorting class due to an
                     * unresolved issue within react-sortable-hoc
                     * @see https://github.com/clauderic/react-sortable-hoc/issues/334
                     **/
                    "sortable-array-field-list-group-item" : true,
                    "sortable-array-field-list-group-item-sorting" : isListGroupSorting,
                    "sortable-array-field-list-group-item-last-touched" : isLastTouched
                }) }
            >
                { buttons }
                <ScrollIntoViewIfNeeded active={ scrollTo }>
                    <DragHandle
                        isCursorActive={ isCursorActive }
                    >
                        { children }
                    </DragHandle>
                </ScrollIntoViewIfNeeded>
            </ListGroupItem>
        );
    }
}

export default ListItem;
