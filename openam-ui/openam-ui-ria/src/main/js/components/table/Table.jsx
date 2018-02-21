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

import { pluck, without } from "lodash";
import { BootstrapTable } from "react-bootstrap-table";
import React, { PropTypes } from "react";

import RowSelection from "components/table/selection/RowSelection";

const Table = ({ idField, onRowClick, onSelectedChange, selectedIds, tableRef, ...restProps }) => {
    const handleSelect = (row, isSelected) => {
        const id = row[idField];
        const selected = isSelected ? [...selectedIds, id] : without(selectedIds, id);

        onSelectedChange(selected);
    };
    const handleSelectAll = (isSelected, rows) => onSelectedChange(isSelected ? pluck(rows, idField) : []);

    return (
        <BootstrapTable
            options={ {
                onRowClick,
                sortIndicator: false
            } }
            ref={ tableRef }
            selectRow={ {
                bgColor: "#f7f7f7",
                className: "active",
                columnWidth: "50px",
                customComponent: RowSelection,
                mode: "checkbox",
                onSelect: handleSelect,
                onSelectAll: handleSelectAll,
                selected: selectedIds
            } }
            { ...restProps }
        />
    );
};

Table.defaultProps = {
    bordered: false,
    condensed: false,
    hover: true
};

Table.propTypes = {
    bordered: PropTypes.bool,
    condensed: PropTypes.bool,
    hover: PropTypes.bool,
    idField: PropTypes.string.isRequired,
    onRowClick: PropTypes.func.isRequired,
    onSelectedChange: PropTypes.func.isRequired,
    selectedIds: PropTypes.arrayOf(PropTypes.any).isRequired,
    tableRef: PropTypes.func
};

export default Table;
