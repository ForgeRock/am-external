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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { camelCase, each, isArray } from "lodash";

import Router from "org/forgerock/commons/ui/common/main/Router";

function throwOnNoData (data) {
    if (!data) {
        throw new Error("[createTreeNavigation] No \"data\" array found.");
    } else if (data && !isArray(data)) {
        throw new Error("[createTreeNavigation] \"data\" is not an array.");
    }
}

function throwOnArgsNotArray (args) {
    if (args && !isArray(args)) {
        throw new Error("[createTreeNavigation] \"args\" is not an array.");
    }
}

/**
 * @param  {Array<object>} data An array of navigation objects
 * @param  {string} data[].title The navigation link title. This can be a translation string.
 * @param  {string} data[].icon The navigation link icon
 * @param  {string} [data[].route] Each data[] object needs to have one of the following,
 *                                 [data[].route], or [data[].event], or [data[].children].
 *                                 If a [data[].route] is supplied, the Router will convert this value into an href
 * @param  {string} [data[].event] If a [data[].event] is supplied, the event will be added via the template
 * @param  {Array} [data[].children] An array of child navigation objects of the same format as this.
 * @param  {Array} [args] An array of routing arguments.
 * @example
 * <code>
 * data = [{
 *     title: "console.navigation.foo",
 *     icon: "fa-check-triangle-o",
 *     route: "myRouteKey"
 * }, {
 *     title: "console.navigation.privileges",
 *     icon: "fa-check-square-o",
 *     event: "main.navigation.EVENT_REDIRECT_TO_JATO_PRIVILEGES"
 * }, {
 *     title: "console.navigation.authorization",
 *     icon: "fa-key",
 *     children: [{
 *         title: "console.navigation.policySets",
 *         icon: "fa-angle-right",
 *         route: "realmsPolicySets"
 *     }, {
 *         title: "console.navigation.resourceTypes",
 *         icon: "fa-angle-right",
 *         route: "realmsResourceTypes"
 *     }]
 * }]
 * </code>
 * @returns {Array<object>} Returns the same array with an object[].href added to each navigation object.
 */
function createTreeNavigation (data, args) {
    throwOnNoData(data);
    throwOnArgsNotArray(args);

    each(data, (navObj) => {
        if (navObj.route) {
            const route = Router.configuration.routes[navObj.route];
            if (route) {
                navObj.href = `#${Router.getLink(route, args)}`;
            } else {
                throw new Error(`[createTreeNavigation] Route "${navObj.route}" does not exist.`);
            }
        } else if (navObj.event) {
            navObj.href = "#";
        } else if (navObj.children) {
            navObj.href = camelCase(navObj.title);
            createTreeNavigation(navObj.children, args);
        }
    });
    return data;
}
export default createTreeNavigation;
