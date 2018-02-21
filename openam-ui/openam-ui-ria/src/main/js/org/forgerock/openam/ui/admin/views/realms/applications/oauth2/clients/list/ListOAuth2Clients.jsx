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

import { Panel } from "react-bootstrap";
import { isEmpty, isEqual, pluck } from "lodash";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
import React, { Component, PropTypes } from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import IconCell from "components/table/cells/IconCell";
import ListOAuth2ClientsCallToAction from "./ListOAuth2ClientsCallToAction";
import ListOAuth2Toolbar from "./ListOAuth2Toolbar";
import Loading from "components/Loading";
import StatusCell from "components/table/cells/StatusCell";
import Table from "components/table/Table";

class ListOAuth2Clients extends Component {
    constructor () {
        super();
        this.state = { selectedIds: [] };
    }

    componentWillReceiveProps (nextProps) {
        const clientsSetHasChanged = !isEqual(
            pluck(this.props.clients, "_id").sort(),
            pluck(nextProps.clients, "_id").sort()
        );

        if (clientsSetHasChanged) {
            // deselect everything in case of pagination, items deletion, etc.
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
        } else if (isEmpty(this.props.clients)) {
            content = <ListOAuth2ClientsCallToAction href={ this.props.newHref } />;
        } else {
            toolbar = (
                <ListOAuth2Toolbar
                    isDeleteDisabled={ !this.state.selectedIds.length }
                    newHref={ this.props.newHref }
                    numberSelected={ this.state.selectedIds.length }
                    onDelete={ handleOnDelete }
                />
            );
            content = (
                <Table
                    data={ this.props.clients }
                    idField="_id"
                    onRowClick={ handleRowClick }
                    onSelectedChange={ handleSelectedChange }
                    selectedIds={ this.state.selectedIds }
                >
                    <TableHeaderColumn dataField="_id" dataFormat={ IconCell("fa-list-alt") } dataSort isKey>
                        { t("console.applications.oauth2.clients.list.grid.0") }
                    </TableHeaderColumn>
                    <TableHeaderColumn
                        dataField="coreOAuth2ClientConfig"
                        dataFormat={ dataFieldObjectPath((cell) => cell, "agentgroup") }
                        dataSort
                    >
                        { t("console.applications.oauth2.clients.list.grid.1") }
                    </TableHeaderColumn>
                    <TableHeaderColumn
                        dataField="coreOAuth2ClientConfig"
                        dataFormat={ dataFieldObjectPath(StatusCell, "status") }
                        dataSort
                    >
                        { t("console.applications.oauth2.clients.list.grid.2") }
                    </TableHeaderColumn>
                </Table>
            );
        }

        return (
            <Panel className="fr-panel-tab">
                { toolbar }
                { content }
            </Panel>
        );
    }
}

ListOAuth2Clients.propTypes = {
    clients: PropTypes.arrayOf(PropTypes.object).isRequired,
    isFetching: PropTypes.bool.isRequired,
    newHref: PropTypes.string.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired
};

export default ListOAuth2Clients;
