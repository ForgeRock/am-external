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
 * Copyright 2011-2019 ForgeRock AS.
 */

import _ from "lodash";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import Footer from "Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import Queue from "org/forgerock/commons/ui/common/util/Queue";
import removeLocalUserData from "org/forgerock/openam/ui/user/login/removeLocalUserData";
import RESTLoginDialog from "org/forgerock/openam/ui/user/login/RESTLoginDialog";
import Router from "org/forgerock/commons/ui/common/main/Router";
import RouteTo from "org/forgerock/openam/ui/common/RouteTo";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";

export default [{
    startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
    processDescription () {
        const routerParams = {
            trigger: true,
            replace: true
        };

        if (!Configuration.loggedUser) {
            Router.routeTo(Router.configuration.routes.login, routerParams);
        } else if (_.includes(Configuration.loggedUser.uiroles, "ui-realm-admin")) {
            Router.routeTo(Router.configuration.routes.realms, {
                args: [],
                ...routerParams
            });
        } else {
            Router.routeTo(Router.configuration.routes.profile, routerParams);
        }
    }
}, {
    startEvent: Constants.EVENT_THEME_CHANGED,
    processDescription () {
        Footer.render();
        LoginHeader.render();
    }
}, {
    startEvent: Constants.EVENT_AUTHENTICATED,
    processDescription () {
        const queueName = "loginDialogAuthCallbacks";
        let authenticatedCallback;

        if (Configuration.globalData[queueName]) {
            authenticatedCallback = Configuration.globalData[queueName].remove();
        }

        if (Configuration.loggedUser && Configuration.globalData.xuiUserSessionValidationEnabled &&
            !Configuration.loggedUser.hasRole(["ui-realm-admin", "ui-global-admin"])) {
            SessionValidator.start(MaxIdleTimeLeftStrategy);
        }

        while (authenticatedCallback) {
            authenticatedCallback();
            authenticatedCallback = Configuration.globalData[queueName].remove();
        }
    }
}, {
    startEvent: Constants.EVENT_UNAUTHORIZED,
    processDescription (event) {
        if (!Configuration.loggedUser) {
            // 401 no session
            return RouteTo.logout();
        } else if (_.get(event, "fromRouter")) {
            // 403 route change
            return RouteTo.forbiddenPage();
        } else {
            // 403 rest call
            return RouteTo.forbiddenError();
        }
    }
}, {
    startEvent: Constants.EVENT_SHOW_LOGIN_DIALOG,
    processDescription (event) {
        const queueName = "loginDialogAuthCallbacks";

        /**
         * EVENT_SHOW_LOGIN_DIALOG may be invoked multiple times. The presence of user is explicitly checked because
         * there should still be a user present if the user's session has expired (and not purposefully logged out).
         * This is required to protect against when this function is invoked multiple times, the first time
         * invoking the first condition and triggering a logout (and clearing Configuration.loggedUser), meaning
         * subsequent invocations incorrectly trigger the second condition.
         */
        if (Configuration.loggedUser) {
            if (Configuration.loggedUser.hasRole("ui-self-service-user")) {
                /**
                 * User may have sensetive information on screen so we exit them from the system when their session
                 * has expired with a message telling them as such
                 */
                removeLocalUserData();
                Router.routeTo(Router.configuration.routes.sessionExpired, { trigger: true });
            } else {
                /**
                 * Admins are more likely to have work in-progress so they are presented with a login dialog to give
                 * them the opportunity to continue their work
                 */

                if (!Configuration.globalData[queueName]) {
                    Configuration.globalData[queueName] = new Queue();
                }

                // only render the RESTLoginDialog if it has an empty callback queue
                if (!Configuration.globalData[queueName].peek()) {
                    RESTLoginDialog.render();
                }
                if (event.authenticatedCallback) {
                    Configuration.globalData[queueName].add(event.authenticatedCallback);
                }
            }
        }
    }
}];
