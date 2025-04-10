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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import Backbone from "backbone";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
 * @exports org/forgerock/commons/ui/common/main/Router
 */
const Router = {
    configuration: {
        routes: {}
    }
};

Router.currentRoute = {};

// returns undecoded route parameters for the provided hash
Router.extractParameters = function (route, hash) {
    if (_.isRegExp(route.url)) {
        return route.url.exec(hash).slice(1);
    } else {
        return null;
    }
};

/**
 * Given a route and a set of current parameters, will return a new parameter
 * list with whichever missing default values were available.
 */
Router.applyDefaultParameters = function (route, originalParameters) {
    let populatedParameters = _.clone(originalParameters);
    let maxArgLength;
    let i;

    if (_.isObject(route.defaults)) {
        if (_.isArray(originalParameters) && originalParameters.length) {
            maxArgLength = (originalParameters.length >= route.defaults.length)
                ? originalParameters.length : route.defaults.length;
            for (i = 0; i < maxArgLength; i++) {
                if (!_.isString(originalParameters[i]) && !_.isUndefined(route.defaults[i])) {
                    populatedParameters[i] = _.clone(route.defaults[i]);
                }
            }
        } else {
            populatedParameters = _.clone(route.defaults);
        }
    }
    return populatedParameters;
};

Router.isRoleAuthorized = function (role) {
    if (!role) { return true; }

    if (Configuration.loggedUser) {
        return Configuration.loggedUser.uiroles.includes(role);
    } else {
        return false;
    }
};

Router.init = function (routes) {
    Router.configuration.routes = routes;

    const CustomRouter = Backbone.Router.extend({
        initialize (routes) {
            _.each(routes, _.bind(function (route, key) {
                this.route(route.url, key, _.partial(this.routeCallback, route));
            }, this));
        },
        routeCallback (route) {
            if (!Router.isRoleAuthorized(route.role)) {
                if (Configuration.loggedUser) {
                    EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, { fromRouter: true });
                } else {
                    EventManager.sendEvent(Constants.EVENT_UNAUTHENTICATED, { fromRouter: true });
                }
                return;
            }

            /**
             * we don't actually use any of the backbone-provided arguments to this function,
             * as they are decoded and that results in the loss of important context.
             * instead we parse the parameters out of the hash ourselves:
             */
            const args = Router.applyDefaultParameters(route, Router.extractParameters(route, URIUtils.getCurrentFragment()));

            Router.currentRoute = route;

            if (route.event) {
                EventManager.sendEvent(route.event, { route, args });
            } else if (route.view) {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, { route, args, fromRouter: true });
            }
        }
    });

    Router.router = new CustomRouter(Router.configuration.routes);
    Backbone.history.start();
};

Router.routeTo = function (route, params) {
    let link;

    if (params && params.args) {
        link = Router.getLink(route, params.args);
    } else if (_.isArray(route.defaults) && route.defaults.length) {
        link = Router.getLink(route, route.defaults);
    } else {
        link = route.url;
    }

    Router.currentRoute = route;
    Router.router.navigate(link, params);
};

Router.navigate = function (link, params) {
    Router.router.navigate(link, params);
};

Router.getLink = function (route, rawParameters) {
    let pattern;
    const args = Router.applyDefaultParameters(route, rawParameters);
    let i = 0;

    if (!_.isRegExp(route.url)) {
        pattern = route.url.replace(/:[A-Za-z@.]+/, "?");
    } else {
        pattern = route.pattern;
    }

    if (args) {
        // Breaks the pattern up into groups, based on ? placeholders
        // Each ? found will be replaced by the corresponding argument
        // The final result will recompose the groups into a single string value
        pattern = _.map(pattern.match(/([^\?]+|\?)/g), (part) => {
            if (part === "?") {
                return (typeof args[i] === "string") ? args[i++] : "";
            } else {
                return part;
            }
        }).join("");
    }

    return pattern;
};

export default Router;
