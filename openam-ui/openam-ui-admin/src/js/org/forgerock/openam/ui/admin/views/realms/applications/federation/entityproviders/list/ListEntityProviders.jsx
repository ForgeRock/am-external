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
 * Copyright 2018-2024 ForgeRock AS.
 */

import { Button, ButtonToolbar } from "react-bootstrap";
import { t } from "i18next";
import { identity, upperFirst, omit } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import AddButtonContainer from "./AddButtonContainer";
import dataFormatReact from "components/table/cells/dataFormatReact";
import DeleteModal from "components/dialogs/Delete";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import formatLocale from "./formatLocale";
import ListEntityProvidersCallToAction from "./ListEntityProvidersCallToAction";
import Table from "components/table/Table";
import SearchFieldWithReset from "org/forgerock/openam/ui/admin/views/realms/common/SearchFieldWithReset";
import EmphasizedTextCell from "components/table/cells/EmphasizedTextCell";

class ListEntityProviders extends Component {
    static propTypes = {
        isSearching: PropTypes.bool.isRequired,
        items: PropTypes.arrayOf(PropTypes.shape({
            _id: PropTypes.string,
            entityId: PropTypes.string,
            location: PropTypes.string
        })).isRequired,
        onDelete: PropTypes.func.isRequired,
        onRowClick: PropTypes.func.isRequired,
        onSearchKeyPress: PropTypes.func.isRequired
    };
    state = {
        selectedItems: [],
        showDeleteDialog: false,
        searchTerm: ""
    };

    handleSearchClear = () => {
        this.setState({ searchTerm: "" }, () => {
            this.props.onSearchKeyPress(this.state.searchTerm);
        });
    };

    handleSearchChange = (event) => {
        this.setState({ searchTerm: event.currentTarget.value });
    };

    handleSearchKeyPress = (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            this.setState({ searchTerm: event.currentTarget.value });
            this.props.onSearchKeyPress(this.state.searchTerm);
        }
    };

    handleCancelDelete = () => this.setState({ showDeleteDialog: false });

    handleConfirmDelete = () => {
        return this.props.onDelete(this.state.selectedItems)
            .then(() => this.setState({ selectedItems: [] }))
            .finally(() => this.setState({ showDeleteDialog: false }));
    };

    handleDelete = () => this.setState({ showDeleteDialog: true });

    handleSelectedChange = (items) => this.setState({ selectedItems: items });

    render () {
        const columns = [{
            title: identity,
            dataField: "entityId",
            formatter: dataFormatReact(
                <FontAwesomeIconCell icon="building" >
                    <EmphasizedTextCell match={ this.state.searchTerm } />
                </FontAwesomeIconCell>
            ),
            sort: true,
            text: t("console.applications.federation.entityProviders.list.grid.0")
        }, {
            title: formatLocale,
            dataField: "roles",
            formatter: formatLocale,
            text: t("console.applications.federation.entityProviders.list.grid.1")
        }, {
            dataField: "location",
            formatter: upperFirst,
            sort: true,
            text: t("console.applications.federation.entityProviders.list.grid.2")
        }];
        let content;
        if (this.props.items.length) {
            const numberSelectedText = this.state.selectedItems.length ? `(${this.state.selectedItems.length})` : "";
            content = (
                <>
                    <ButtonToolbar className="page-toolbar">
                        <AddButtonContainer />
                        <Button disabled={ !this.state.selectedItems.length } onClick={ this.handleDelete }>
                            <i className="fa fa-close" /> { t("common.form.delete") } { numberSelectedText }
                        </Button>
                    </ButtonToolbar>
                    <Table
                        { ...omit(this.props, "children") }
                        columns={ columns }
                        data={ this.props.items }
                        keyField={ "_id" }
                        onRowClick={ this.props.onRowClick }
                        onSelectedChange={ this.handleSelectedChange }
                        selectedItems={ this.state.selectedItems }
                    />
                </>
            );
        } else {
            content = this.props.isSearching
                ? t("console.applications.federation.entityProviders.list.noSuchEntity")
                : <ListEntityProvidersCallToAction />;
        }

        return (
            <>
                <SearchFieldWithReset
                    isResetEnabled={ !!this.state.searchTerm }
                    label={ t("console.applications.federation.entityProviders.list.search") }
                    onChange={ this.handleSearchChange }
                    onClear={ this.handleSearchClear }
                    onKeyPress={ this.handleSearchKeyPress }
                    placeholder={ t("console.applications.federation.entityProviders.list.search") }
                    value={ this.state.searchTerm }
                />
                { content }
                <DeleteModal
                    names={ this.state.selectedItems.map((item) => item.entityId) }
                    objectName="entityProvider"
                    onCancel={ this.handleCancelDelete }
                    onConfirm={ this.handleConfirmDelete }
                    show={ this.state.showDeleteDialog }
                />
            </>
        );
    }
}

export default ListEntityProviders;
