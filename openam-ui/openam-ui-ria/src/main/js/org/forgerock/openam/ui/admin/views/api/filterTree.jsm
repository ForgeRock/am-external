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
