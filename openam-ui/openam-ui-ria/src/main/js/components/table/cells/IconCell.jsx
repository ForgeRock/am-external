/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import React from "react";

const Cell = (faClassName, cell) => (
    <span className="am-table-icon-cell">
        <span className="fa-stack fa-lg am-table-icon-cell-stack">
            <i className="fa fa-circle fa-stack-2x text-primary" />
            <i className={ `fa ${faClassName} fa-stack-1x fa-inverse` } />
        </span>
        { " " }
        <span>{ cell }</span>
    </span>
);

const IconCell = (faClassName) => (cell) => Cell(faClassName, cell);

export default IconCell;
