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
 * Copyright 2011-2018 ForgeRock AS.
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
var Router = {
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
    var populatedParameters = _.clone(originalParameters),
        maxArgLength,i;

    if (_.isObject(route.defaults)) {
        if (_.isArray(originalParameters) && originalParameters.length) {
            maxArgLength = (originalParameters.length >= route.defaults.length)
                ? originalParameters.length : route.defaults.length;
            for (i=0;i<maxArgLength;i++) {
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

Router.checkRole = function (route) {
    if(route.role) {
        if (Configuration.loggedUser) {
            const hasAutherizedRole = _.includes(Configuration.loggedUser.uiroles, route.role);
            if (!hasAutherizedRole) {
                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, { fromRouter: true });
                return false;
            }
        } else {
            EventManager.sendEvent(Constants.EVENT_UNAUTHENTICATED, { fromRouter: true });
            return false;
        }
    }
    return true;
};

Router.init = function(routes) {
    Router.configuration.routes = routes;

    var CustomRouter = Backbone.Router.extend({
        initialize: function(routes) {
            _.each(routes, function(route, key) {
                this.route(route.url, key, _.partial(this.routeCallback, route));
            }, this);
        },
        routeCallback : function(route) {
            if (!Router.checkRole(route)) {
                if (!Configuration.loggedUser) {
                    return;
                }
                route = Router.configuration.routes.unauthorized;
            }

            /**
             * we don't actually use any of the backbone-provided arguments to this function,
             * as they are decoded and that results in the loss of important context.
             * instead we parse the parameters out of the hash ourselves:
             */
            var args = Router.applyDefaultParameters(route, Router.extractParameters(route, URIUtils.getCurrentFragment()));

            Router.currentRoute = route;

            if(route.event) {
                EventManager.sendEvent(route.event, {route: route, args: args});
            } else if(route.view) {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: route, args: args, fromRouter: true});
            }
        }
    });

    Router.router = new CustomRouter(Router.configuration.routes);
    Backbone.history.start();
};

Router.routeTo = function(route, params) {
    var link;

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

Router.navigate = function(link, params) {
    Router.router.navigate(link, params);
};

Router.getLink = function(route, rawParameters) {
    var pattern,
        args = Router.applyDefaultParameters(route, rawParameters),
        i = 0;

    if (!_.isRegExp(route.url)) {
        pattern = route.url.replace(/:[A-Za-z@.]+/, "?");
    } else {
        pattern = route.pattern;
    }

    if (args) {
        // Breaks the pattern up into groups, based on ? placeholders
        // Each ? found will be replaced by the corresponding argument
        // The final result will recompose the groups into a single string value
        pattern = _.map(pattern.match(/([^\?]+|\?)/g), function (part) {
            if (part === "?") {
                return (typeof args[i] === "string") ? args[i++] : "";
            } else {
                return part;
            }
        }).join('');
    }

    return pattern;
};

export default Router;
