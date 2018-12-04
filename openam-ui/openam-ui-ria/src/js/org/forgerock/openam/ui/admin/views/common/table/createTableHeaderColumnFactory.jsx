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
 * Copyright 2018 ForgeRock AS.
 */

import { TableHeaderColumn } from "react-bootstrap-table";
import moment from "moment";
import React from "react";

import dateTimeEditor from "./editors/string/dateTimeEditor";
import enumArrayEditor from "./editors/array/enumArrayEditor";
import enumArrayFormat from "./editors/array/enumArrayFormat";
import enumEditor from "./editors/enum/enumEditor";
import enumFormat from "./editors/enum/enumFormat";
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
            getElement: enumEditor,
            customEditorParameters: { schema }
        };
        dataFormat = (cell) => enumFormat(cell, schema);
    } else if (schema.type === "array") {
        if (schema.items && schema.items.enum) {
            customEditor = {
                getElement: enumArrayEditor,
                customEditorParameters: { schema }
            };
            dataFormat = (cell) => enumArrayFormat(cell, schema);
        } else {
            // TODO - Createable array editor for inline tables
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
