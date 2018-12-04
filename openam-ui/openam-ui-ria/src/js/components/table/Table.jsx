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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { BootstrapTable } from "react-bootstrap-table";
import { findWhere, pluck, without } from "lodash";
import PropTypes from "prop-types";
import React from "react";

import RowSelection from "components/table/selection/RowSelection";

const Table = ({ keyField, onRowClick, onSelectedChange, options = {}, selectedItems, tableRef, ...restProps }) => {
    const handleSelect = (row, isSelected) => {
        const selected = isSelected
            ? [...selectedItems, row]
            : without(selectedItems, findWhere(selectedItems, { [keyField]: row[keyField] }));

        onSelectedChange(selected);
    };
    const handleSelectAll = (isSelected, rows) => onSelectedChange(isSelected ? rows : []);
    const fetchInfo = options.dataTotalSize ? { dataTotalSize: options.dataTotalSize } : undefined;

    return (
        <BootstrapTable
            fetchInfo={ fetchInfo }
            keyField={ keyField }
            options={ {
                onRowClick,
                ...options
            } }
            pagination={ options.pagination }
            ref={ tableRef }
            remote={ options.remote }
            selectRow={ {
                bgColor: "#f7f7f7",
                className: "active",
                columnWidth: "50px",
                customComponent: RowSelection,
                mode: "checkbox",
                onSelect: handleSelect,
                onSelectAll: handleSelectAll,
                selected: pluck(selectedItems, keyField)
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
    keyField: PropTypes.string.isRequired,
    onRowClick: PropTypes.func,
    onSelectedChange: PropTypes.func.isRequired,
    options: PropTypes.objectOf(PropTypes.any),
    selectedItems: PropTypes.arrayOf(PropTypes.any).isRequired,
    tableRef: PropTypes.func
};

export default Table;
