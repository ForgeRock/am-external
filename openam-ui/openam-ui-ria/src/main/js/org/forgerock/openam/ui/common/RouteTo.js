/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
