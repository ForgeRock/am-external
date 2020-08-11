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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { TableHeaderColumn } from "react-bootstrap-table";
import moment from "moment";
import React from "react";

import dateTimeEditor from "./editors/dateTimeEditor";
import creatableMultiSelectEditor from "./editors/creatableMultiSelectEditor";
import multiSelectEditor from "./editors/multiSelectEditor";
import singleSelectEditor from "./editors/singleSelectEditor";

import enumArrayFormat from "./formats/enumArrayFormat";
import enumFormat from "./formats/enumFormat";
import arrayFormat from "./formats/arrayFormat";
import TitleWithPopover from "components/table/column/TitleWithPopover";

/**
 * @module org/forgerock/openam/ui/admin/views/common/table/createTableHeaderColumnFactory
 */

/**
 * Given a jsonschema property, this module will returns a TableHeaderColumn react element that contains the correct
 * editors and formating for each cell.
 * @param {Object} schema The jsonschema property
 * @param {string} key The jsonschema property name
 * @returns {ReactElement} Renderable React element
 */
const createTableHeaderColumnFactory = (schema, key) => {
    let customEditor;
    let dataFormat;

    if (schema.enum) {
        customEditor = {
            getElement: singleSelectEditor,
            customEditorParameters: { schema }
        };
        dataFormat = (cell) => enumFormat(cell, schema);
    } else if (schema.type === "array" && schema.items) {
        if (schema.items.enum) {
            customEditor = {
                getElement: multiSelectEditor,
                customEditorParameters: { schema }
            };
            dataFormat = (cell) => enumArrayFormat(cell, schema);
        } else {
            customEditor = {
                getElement: creatableMultiSelectEditor,
                customEditorParameters: { schema }
            };
            dataFormat = (cell) => arrayFormat(cell);
        }
    } else if (schema.format === "date-time") {
        customEditor = {
            getElement: dateTimeEditor,
            customEditorParameters: { schema }
        };
        dataFormat = (cell) => moment(cell, moment.ISO_8601).utc().format("DD-MMM YYYY HH:mm");
    }
    return (
        <TableHeaderColumn
            columnClassName="editable-cell"
            customEditor={ customEditor }
            dataField={ key }
            dataFormat={ dataFormat }
            dataSort
            editColumnClassName="editing-cell"
            key={ key }
        >
            <TitleWithPopover popover={ schema.description } title={ schema.title } />
        </TableHeaderColumn>
    );
};

export default createTableHeaderColumnFactory;
