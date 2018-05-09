/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
import { get } from "lodash";

/**
 * HoF (Higher-order function) to wrap a React Bootstrap Table dataFormat function, allowing it
 * to receive specific attributes from within an object cell.
 *
 * Necessary as RBT does not provide addressing of a sub-object via the "dataField" attribute.
 * @param {function} wrappedFunction dataFormat function to wrap
 * @param {String} path Path upon the cell object to provide the wrapped function
 * @returns {function} Wrapped function
 * @example
 * import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
 *
 * <Table>
 *     <TableHeaderColumn
 *         dataField="general" // "general" is an object with a attribute of "status"
 *         dataFormat={ dataFieldObjectPath(StatusCell, "status") }>
 *      ...
 *      </TableHeaderColumn>
 *  </Table>
 * @see https://github.com/AllenFang/react-bootstrap-table/issues/50
 */
const dataFieldObjectPath = (wrappedFunction, path) => {
    return (cell, row, formatExtraData, rowIdx) => {
        return wrappedFunction(get(cell, path), row, formatExtraData, rowIdx);
    };
};

export default dataFieldObjectPath;
