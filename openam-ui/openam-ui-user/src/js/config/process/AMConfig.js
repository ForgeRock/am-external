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
 * Copyright 2011-2025 Ping Identity Corporation.
 */

import _ from "lodash";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Footer from "org/forgerock/openam/ui/common/components/Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import logout from "org/forgerock/openam/ui/user/login/logout";
import redirectToAdmin from "org/forgerock/openam/ui/common/redirectToAdmin";
import removeLocalUserData from "org/forgerock/openam/ui/user/login/removeLocalUserData";
import Router from "org/forgerock/commons/ui/common/main/Router";

export default [{
    startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
    processDescription () {
        const routerParams = {
            trigger: true,
            replace: true
        };

        const urlParams = new URLSearchParams(window.location.search);

        if (urlParams.get("suspendedId")) {
            // If url contains a suspendedId query parameter, assume user is
            // attempting to continue a suspended login tree so route to login
            // where the request will be handled by the backend
            Router.routeTo(Router.configuration.routes.login, routerParams);
        } else if (!Configuration.loggedUser) {
            Router.routeTo(Router.configuration.routes.login, routerParams);
        } else if (_.includes(Configuration.loggedUser.uiroles, "ui-realm-admin")) {
            redirectToAdmin();
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
    startEvent: Constants.EVENT_UNAUTHORIZED,
    processDescription (event) {
        if (!Configuration.loggedUser) {
            const routeToLogin = () => {
                Router.routeTo(Router.configuration.routes.login, { trigger: true });
            };
            // 401 no session
            logout().then(routeToLogin, routeToLogin);
        } else if (_.get(event, "fromRouter")) {
            // 403 route change
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: Router.configuration.routes.forbidden, fromRouter: true
            });
        } else {
            // 403 rest call
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
        }
    }
}, {
    startEvent: Constants.EVENT_SHOW_LOGIN_DIALOG,
    processDescription () {
        /**
         * EVENT_SHOW_LOGIN_DIALOG may be invoked multiple times. The presence of user is explicitly checked because
         * there should still be a user present if the user's session has expired (and not purposefully logged out).
         * This is required to protect against when this function is invoked multiple times, the first time
         * invoking the first condition and triggering a logout (and clearing Configuration.loggedUser), meaning
         * subsequent invocations incorrectly trigger the second condition.
         */
        if (Configuration.loggedUser && Configuration.loggedUser.hasRole("ui-self-service-user")) {
            /**
             * User may have sensetive information on screen so we exit them from the system when their session
             * has expired with a message telling them as such
             */
            removeLocalUserData();
            Router.routeTo(Router.configuration.routes.sessionExpired, { trigger: true });
        }
    }
}];
