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
 * Copyright 2015-2018 ForgeRock AS.
 */

/**
  * Provides functions to navigate the application to commonly required routes.
  *
  * @module org/forgerock/openam/ui/common/RouteTo
  */
define([
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/util/URIUtils"
], (Constants, EventManager, Router, Configuration, SessionManager, URIUtils) => {
    var obj = {
        setGotoFragment () {
            var fragment = URIUtils.getCurrentFragment();
            if (!Configuration.gotoFragment && !fragment.match(Router.configuration.routes.login.url)) {
                Configuration.setProperty("gotoFragment", `#${fragment}`);
            }
        },
        forbiddenPage () {
            delete Configuration.globalData.authorizationFailurePending;
            return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: Router.configuration.routes.forbidden,
                fromRouter: true
            });
        },
        forbiddenError () {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
        },
        logout () {
            obj.setGotoFragment();

            return SessionManager.logout().then(() => {
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                    anonymousMode: true
                });
                return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.login
                });
            });
        },
        loginDialog () {
            return EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
        }
    };

    return obj;
});
