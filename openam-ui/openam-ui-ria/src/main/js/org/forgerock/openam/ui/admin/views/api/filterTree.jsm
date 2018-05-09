/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/api/filterTree
 */

import _ from "lodash";

const nestedFilter = (children, filter) => {
    return _.filter(children, (item) => {
        if (item.path && _.includes(item.path.toLowerCase(), filter.toLowerCase())) {
            return true;
        } else if (item.children) {
            item.children = nestedFilter(item.children, filter);
            return !_.isEmpty(item.children);
        }
    });
};

const filterTree = (children, filter = "") => {
    return nestedFilter(_.cloneDeep(children), filter);
};

export default filterTree;
