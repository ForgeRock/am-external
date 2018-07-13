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
import { isEqual, pluck } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import React, { Component, PropTypes } from "react";

import IconCell from "components/table/cells/IconCell";
import ListTreesCallToAction from "./ListTreesCallToAction";
import ListTreesToolbar from "./ListTreesToolbar";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";
import Table from "components/table/Table";

class ListTrees extends Component {
    constructor () {
        super();
        this.state = {
            selectedIds: []
        };
    }

    componentWillReceiveProps (nextProps) {
        const currentIds = pluck(this.props.trees, "_id");
        const nextIds = pluck(nextProps.trees, "_id");

        if (!isEqual(currentIds.sort(), nextIds.sort())) {
            this.setState({ selectedIds: [] });
        }
    }

    render () {
        const handleOnDelete = () => this.props.onDelete(this.state.selectedIds);
        const handleRowClick = (row) => this.props.onEdit(row._id);
        const handleSelectedChange = (ids) => this.setState({ selectedIds: ids });
        let toolbar;
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else if (this.props.trees.length) {
            toolbar = (
                <ListTreesToolbar
                    isDeleteDisabled={ !this.state.selectedIds.length }
                    newHref={ this.props.newHref }
                    numberSelected={ this.state.selectedIds.length }
                    onDelete={ handleOnDelete }
                />
            );
            content = (
                <Table
                    data={ this.props.trees }
                    idField="_id"
                    onRowClick={ handleRowClick }
                    onSelectedChange={ handleSelectedChange }
                    selectedIds={ this.state.selectedIds }
                >
                    <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-tree") } dataSort isKey>
                        { t("console.authentication.trees.list.grid.0") }
                    </TableHeaderColumn>
                </Table>
            );
        } else {
            content = <ListTreesCallToAction href={ this.props.newHref } />;
        }

        return (
            <div>
                <PageHeader title={ t("console.authentication.trees.list.title") } />
                { toolbar }
                <Panel>
                    { content }
                </Panel>
            </div>
        );
    }
}

ListTrees.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    newHref: PropTypes.string.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired,
    trees: PropTypes.arrayOf(PropTypes.object).isRequired
};

export default ListTrees;
