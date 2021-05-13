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
 * Copyright 2019-2020 ForgeRock AS.
 */
import { Button, ButtonGroup, DropdownButton, MenuItem } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import DeleteModal from "components/dialogs/Delete";
import EditEntityPageHeaderRole from "./EditEntityPageHeaderRole";
import Option from "components/inputs/select/components/options/OptionWithDisplayPath";
import PageHeader from "components/PageHeader";
import SearchSingleSelect from "components/inputs/select/SearchSingleSelect";

class EditEntityPageHeader extends Component {
    state = {
        showDeleteDialog: false,
        showDeleteRoleDialog: false
    };

    handleAddRoleClick = () => this.props.onAddRole();

    handleCancelDelete = () => this.setState({ showDeleteDialog: false });

    handleCancelDeleteRole = () => this.setState({ showDeleteRoleDialog: false });

    handleConfirmDelete = () => this.props.onDeleteEntityProvider(this.props.title)
        .finally(() => this.setState({ showDeleteDialog: false }));

    handleConfirmDeleteRole = () => this.props.onDeleteRole(this.props.currentRole)
        .finally(() => this.setState({ showDeleteRoleDialog: false }));

    handleDeleteEntityProvider = () => this.setState({ showDeleteDialog: true });

    handleDelete = () => this.setState({ showDeleteDialog: true });

    handleDeleteRole = () => this.setState({ showDeleteRoleDialog: true });

    handleRoleClick = (role) => this.props.onChangeRole(role);

    handleSearchChange = (option, { action }) => {
        if (action === "select-option") {
            this.props.onSearchChange(option.value);
        }
    };

    render () {
        const roles = this.props.roles.sort().map((role) => {
            const bsStyle = role === this.props.currentRole ? "primary" : "default";
            return (
                <EditEntityPageHeaderRole
                    bsStyle={ bsStyle }
                    key={ role }
                    onClick={ this.handleRoleClick }
                    role={ role }
                >
                    { t(`objects.${role}Abbr`) }
                </EditEntityPageHeaderRole>
            );
        });
        const addRoleButtonTitle = this.props.disableAddRole
            ? t("console.applications.federation.entityProviders.edit.noMoreRoles")
            : undefined;
        const subtitle = (
            <h4 className="page-type">
                { t("console.applications.federation.entityProviders.edit.type") }
                { " | " }
                <span className="text-uppercase">{ this.props.location }</span>
                { " | " }
                <ButtonGroup bsSize="xsmall">
                    { roles }
                </ButtonGroup>
                <ButtonGroup bsSize="xsmall">
                    <Button
                        bsStyle="link"
                        disabled={ this.props.disableAddRole }
                        onClick={ this.handleAddRoleClick }
                        title={ addRoleButtonTitle }
                    >
                        <i className="fa fa-plus" />
                        { " " }
                        { t("console.applications.federation.entityProviders.edit.addRole") }
                    </Button>
                </ButtonGroup>
            </h4>
        );

        const isRoleDeleteDisabled = this.props.location === "remote" || roles.length === 1;

        return (
            <>
                <PageHeader
                    icon="building"
                    subtitle={ subtitle }
                    title={ this.props.title }
                >
                    <SearchSingleSelect
                        components={ { Option } }
                        inputId="editEntityPageHeaderSearch"
                        onChange={ this.handleSearchChange }
                        options={ this.props.options }
                        styles={ {
                            container: (base) => ({
                                ...base,
                                minWidth: 300
                            })
                        } }
                    />
                    <DropdownButton id="editEntityPageHeaderDelete" pullRight title={ t("common.form.delete") }>
                        <MenuItem disabled={ isRoleDeleteDisabled } onSelect={ this.handleDeleteRole }>
                            { t("console.applications.federation.entityProviders.edit.delete.role") }
                        </MenuItem>
                        <MenuItem onSelect={ this.handleDeleteEntityProvider }>
                            { t("console.applications.federation.entityProviders.edit.delete.entityProvider") }
                        </MenuItem>
                    </DropdownButton>
                </PageHeader>
                <DeleteModal
                    names={ [t(`console.applications.federation.entityProviders.roles.${this.props.currentRole}`)] }
                    objectName="role"
                    onCancel={ this.handleCancelDeleteRole }
                    onConfirm={ this.handleConfirmDeleteRole }
                    show={ this.state.showDeleteRoleDialog }
                />
                <DeleteModal
                    names={ [this.props.title] }
                    objectName="entityProvider"
                    onCancel={ this.handleCancelDelete }
                    onConfirm={ this.handleConfirmDelete }
                    show={ this.state.showDeleteDialog }
                />
            </>
        );
    }
}

EditEntityPageHeader.propTypes = {
    currentRole: PropTypes.string.isRequired,
    disableAddRole: PropTypes.bool.isRequired,
    location: PropTypes.string.isRequired,
    onAddRole: PropTypes.func.isRequired,
    onChangeRole: PropTypes.func.isRequired,
    onDeleteEntityProvider: PropTypes.func.isRequired,
    onDeleteRole: PropTypes.func.isRequired,
    onSearchChange: PropTypes.func.isRequired,
    options: PropTypes.arrayOf(PropTypes.object),
    roles: PropTypes.arrayOf(PropTypes.string).isRequired,
    title: PropTypes.string.isRequired
};

export default EditEntityPageHeader;
