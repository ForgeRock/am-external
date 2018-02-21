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

import React, { PropTypes } from "react";
import { map } from "lodash";

import { currentRoute } from "org/forgerock/commons/ui/common/main/Router";
import createBreadcrumbs from "org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs";

/**
 * A Breadcrumb uses the current route to determine it's links and titles. An Optional numOfDroppedFragments can be
 * passed in which will drop the given number of fragments from the beginning of the route. The numOfDroppedFragments
 * defaults to 2 so that a route of "#realms/%2F/authentication-trees/edit/Example" would give the breadcrumb of
 * "Authentication - Trees > edit > Example".
 * Will return 'null' if there are no crumbs returned from the createBreadcrumbs call.
 * @module components/Breadcrumb
 * @param {Number} [numOfDroppedFragments] The number of fragments to drop from the beginning of the hash
 * @returns {ReactElement} Renderable React element or null
 */
const Breadcrumb = ({ numOfDroppedFragments }) => {
    const crumbs = createBreadcrumbs(currentRoute.pattern, numOfDroppedFragments);

    return (
        crumbs ? (
            <ol className="breadcrumb">
                {
                    map(crumbs, ({ path, title }) => {
                        if (path) {
                            return <li><a href={ path }>{ title }</a></li>;
                        } else {
                            return <li>{ title }</li>;
                        }
                    })
                }
            </ol>
        ) : null
    );
};

Breadcrumb.propTypes = {
    numOfDroppedFragments: PropTypes.Number
};

export default Breadcrumb;
