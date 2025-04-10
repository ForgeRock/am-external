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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Badge, Panel } from "react-bootstrap";
import { noop } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import CreateMappingModal from "./CreateMappingModal";
import EditMappingModal from "./EditMappingModal";
import List from "components/list/List";
import TitleWithPopover from "components/table/column/TitleWithPopover";
import { revertPlaceholdersToOriginalValue } from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

class ListMappings extends Component {
    constructor (props) {
        super(props);

        this.state = { showCreateModal: false, showEditModal: false, editRowInstance: {} };
    }

    activeAliasTitle (cell) {
        return cell[0];
    }

    activeAliasDataFormat (cell) {
        return <span>{ cell[0] }</span>;
    }

    aliasesDataFormat (cellLength) {
        return <Badge>{ cellLength }</Badge>;
    }

    handleCreate = (data) => {
        this.props.onCreate(data).then(() => {
            this.setState({ showCreateModal: false });
        }, noop);
    };

    handleCreateModalClose = () => {
        this.setState({ showCreateModal: false });
    };

    handleCreateModalShow = () => {
        this.props.onRefetchSchema();
        this.setState({ showCreateModal: true });
    };

    handleEditModalClose = () => {
        this.setState({ showEditModal: false });
    };

    handleEditModalShow = () => {
        this.props.onRefetchSchema();
        this.setState({ showEditModal: true });
    };

    handleSave = (data, id, schema) => {
        const values = revertPlaceholdersToOriginalValue(data, schema);
        this.props.onSave(values, id).then(() => {
            this.handleEditModalClose();
        }, noop);
    };

    handleTableRowClick = (e, rowData) => {
        this.setState({ editRowInstance: rowData });
        this.handleEditModalShow();
    };

    render () {
        const createMappingModal = this.state.showCreateModal
            ? (
                <CreateMappingModal
                    onClose={ this.handleCreateModalClose }
                    onCreate={ this.handleCreate }
                    schema={ this.props.schema }
                    show={ this.state.showCreateModal }
                    template={ this.props.template }
                />
            )
            : null;

        const editMappingModal = this.state.showEditModal
            ? (
                <EditMappingModal
                    instance={ this.state.editRowInstance }
                    onClose={ this.handleEditModalClose }
                    onSave={ this.handleSave }
                    schema={ this.props.schema }
                    show={ this.state.showEditModal }
                />
            )
            : null;

        const tableHeaderColumns = this.props.schema
            ? [{
                text: "", // Text is a required prop
                columnTitle: true,
                dataField: "secretId",
                headerFormatter: (column, colIndex, components) => { // eslint-disable-line
                    return (
                        <>
                            <TitleWithPopover
                                popover={ this.props.schema.properties.secretId.description }
                                title={ this.props.schema.properties.secretId.title }
                            />
                            { components.sortElement }
                        </>
                    );
                },
                sort: true
            }, {
                text: "", // Text is a required prop
                columnTitle: this.activeAliasTitle,
                dataField: "aliases",
                formatter: this.activeAliasDataFormat,
                headerFormatter: () => {
                    return (
                        <TitleWithPopover
                            popover={ t("console.secretStores.edit.mappings.grid.activeAlias.description") }
                            title={ t("console.secretStores.edit.mappings.grid.activeAlias.title") }
                        />
                    );
                }
            }, {
                text: "", // Text is a required prop
                dataField: "aliases.length",
                formatter: this.aliasesDataFormat,
                align: "center",
                headerFormatter: () => {
                    return (
                        <TitleWithPopover
                            popover={ this.props.schema.properties.aliases.description }
                            title={ this.props.schema.properties.aliases.title }
                        />
                    );
                },
                headerStyle: { width: "100px" }
            }]
            : [];

        return (
            <Panel.Body>
                <List
                    addButton={ {
                        handleOnClick: this.handleCreateModalShow,
                        title: t("console.secretStores.edit.mappings.callToAction.button")
                    } }
                    columns={ tableHeaderColumns }
                    description={ t("console.secretStores.edit.mappings.callToAction.description") }
                    isFetching={ this.props.isFetching }
                    items={ this.props.instances }
                    keyField="secretId"
                    onDelete={ this.props.onDelete }
                    onRowClick={ this.handleTableRowClick }
                    title={ t("console.secretStores.edit.mappings.callToAction.title") }
                />
                { createMappingModal }
                { editMappingModal }
            </Panel.Body>
        );
    }
}

ListMappings.propTypes = {
    instances: PropTypes.arrayOf(PropTypes.object),
    isFetching: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onRefetchSchema: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.any),
    template: PropTypes.objectOf(PropTypes.any)
};

export default ListMappings;
