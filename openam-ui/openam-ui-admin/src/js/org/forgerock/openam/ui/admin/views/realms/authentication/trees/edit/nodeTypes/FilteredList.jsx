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
 * Copyright 2019-2026 Ping Identity Corporation.
 */

import { isEmpty, map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, {
    useEffect,
    useState
} from "react";

import { filterNodesByTag } from "org/forgerock/openam/ui/admin/utils/treeUtils";
import NodeTypeItem from "./NodeTypeItem";

export default function FilteredList ({ filter, nodeTypes }) {
    const [filteredNodeTypes, setFilteredNodeTypes] = useState(nodeTypes);

    useEffect(() => {
        const filtered = filterNodesByTag(nodeTypes, filter);
        setFilteredNodeTypes(filtered);
    }, [filter, nodeTypes]);

    return isEmpty(filteredNodeTypes)
        ? <div className="text-muted text-center">{ t("console.common.noResults") }</div>
        : map(filteredNodeTypes, ({ _id, icon, name, tags, version }) => (
            <NodeTypeItem
                displayName={ name }
                filter={ filter }
                icon={ icon }
                key={ _id }
                nodeType={ _id }
                tags={ tags }
                version={ version }
            />
        ));
}

FilteredList.propTypes = {
    filter: PropTypes.string.isRequired,
    nodeTypes: PropTypes.objectOf(PropTypes.shape({
        _id: PropTypes.string.isRequired,
        icon: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        tags: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired
    })).isRequired
};
