/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { map } from "lodash";
import PropTypes from "prop-types";
import React from "react";

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
