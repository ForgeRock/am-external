/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { BootstrapTable } from "react-bootstrap-table";
import { findWhere, pluck, without } from "lodash";
import PropTypes from "prop-types";
import React from "react";

import RowSelection from "components/table/selection/RowSelection";

const Table = ({ idField, onRowClick, onSelectedChange, options = {}, selectedItems, tableRef, ...restProps }) => {
    const handleSelect = (row, isSelected) => {
        const selected = isSelected
            ? [...selectedItems, row]
            : without(selectedItems, findWhere(selectedItems, { _id: row[idField] }));

        onSelectedChange(selected);
    };
    const handleSelectAll = (isSelected, rows) => onSelectedChange(isSelected ? rows : []);
    const fetchInfo = options.dataTotalSize ? { dataTotalSize: options.dataTotalSize } : undefined;

    return (
        <BootstrapTable
            fetchInfo={ fetchInfo }
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
                selected: pluck(selectedItems, idField)
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
    options: PropTypes.objectOf(PropTypes.any),
    selectedItems: PropTypes.arrayOf(PropTypes.any).isRequired,
    tableRef: PropTypes.func
};

export default Table;
