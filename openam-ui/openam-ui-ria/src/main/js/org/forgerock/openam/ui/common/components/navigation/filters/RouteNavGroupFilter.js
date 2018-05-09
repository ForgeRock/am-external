/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * Filters links on the navigation group of the current route.
 *
 * @module org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter
 */
define([
    "org/forgerock/commons/ui/common/main/Router"
], (Router) => {
    return {
        filter (links) {
            if (Router.currentRoute.navGroup) {
                return links[Router.currentRoute.navGroup];
            }
        }
    };
});
