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
