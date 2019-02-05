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

import { exists, remove, setValidated, toHref, validateParam } from "org/forgerock/openam/ui/user/login/gotoUrl";
import { global as legacyConsoleRedirectGlobal } from "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole";
import { hideAPILinksOnAPIDescriptionsDisabled, populateRealmsDropdown } from
    "org/forgerock/openam/ui/common/util/NavigationHelper";
import { parseParameters } from "org/forgerock/openam/ui/common/util/uri/query";
import { removeRealm } from "store/modules/local/session";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Footer from "Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import logout from "org/forgerock/openam/ui/user/login/logout";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import Queue from "org/forgerock/commons/ui/common/util/Queue";
import RealmsService from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import RESTLoginDialog from "org/forgerock/openam/ui/user/login/RESTLoginDialog";
import Router from "org/forgerock/commons/ui/common/main/Router";
import RouteTo from "org/forgerock/openam/ui/common/RouteTo";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import store from "store";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

export default [{
    startEvent: Constants.EVENT_LOGOUT,
    processDescription () {
        SessionValidator.stop();

        const logoutSuccess = (response) => {
            store.dispatch(removeRealm());
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });

            // Use logout response (if there is one) in preference to supplied goto param.
            // Requires any post process url endpoint to redirect to the goto url after it completes
            if (_.has(response, "goto")) {
                window.location.href = decodeURIComponent(response.goto);
            } else if (exists()) {
                window.location.href = toHref();
            } else {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.loggedOut
                });
            }
        };

        const logoutFail = () => {
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");

            if (exists()) {
                window.location.href = toHref();
            } else {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.login
                });
            }
        };

        const unvalidatedGotoParam = parseParameters(URIUtils.getCurrentFragment()).goto ||
        parseParameters(URIUtils.getCurrentQueryString()).goto;
        if (unvalidatedGotoParam) {
            validateParam(unvalidatedGotoParam).then((validatedUrl) => {
                setValidated(validatedUrl);
                logout().then(logoutSuccess, logoutFail);
            }, () => {
                remove();
                logout().then(logoutSuccess, logoutFail);
            });
        } else {
            remove();
            logout().then(logoutSuccess, logoutFail);
        }
    }
}, {
    startEvent: Constants.EVENT_REDIRECT_TO_JATO_FEDERATION,
    processDescription () {
        legacyConsoleRedirectGlobal.federation();
    }
}, {
    startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
    processDescription () {
        const routerParams = {
            trigger: true,
            replace: true
        };

        if (!Configuration.loggedUser) {
            Router.routeTo(Router.configuration.routes.login, routerParams);
        } else if (_.contains(Configuration.loggedUser.uiroles, "ui-realm-admin")) {
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

        if (Configuration.loggedUser && Configuration.loggedUser.hasRole("ui-realm-admin")) {
            RealmsService.realms.all().then(populateRealmsDropdown);
            const suppressError = { errorsHandlers : { "Forbidden": { status: 403 } } };
            ServicesService.instance.get("rest", suppressError)
                .then(hideAPILinksOnAPIDescriptionsDisabled);
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
                // TODO move the logout logic to the Session Expiry view
                return logout().then(() => {
                    Router.routeTo(Router.configuration.routes.sessionExpired, { trigger: true });
                });
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