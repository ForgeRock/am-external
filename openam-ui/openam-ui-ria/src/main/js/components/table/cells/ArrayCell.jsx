/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Creatable } from "react-select";
import { map } from "lodash";
import React from "react";

const ArrayCell = (items) => {
    return (
        <Creatable
            className="am-react-select-array-cell"
            clearable={ false }
            disabled
            multi
            searchable={ false }
            value={ map(items, (data) => ({ label: data, value: data })) }
        />
    );
};

export default ArrayCell;
