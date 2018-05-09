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

define([
    "jquery",
    "lodash",
    "Footer",
    "org/forgerock/commons/ui/common/components/LoginHeader",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole",
    "org/forgerock/openam/ui/common/sessions/SessionValidator",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/uri/query",
    "org/forgerock/openam/ui/user/login/gotoUrl",
    "org/forgerock/openam/ui/user/login/logout",
    "store/index",
    "store/modules/local/session",
    "org/forgerock/openam/ui/admin/services/global/RealmsService",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy",
    "org/forgerock/openam/ui/common/util/NavigationHelper",
    "org/forgerock/openam/ui/common/RouteTo",
    "LoginDialog",
    "org/forgerock/commons/ui/common/util/Queue"
], ($, _, Footer, LoginHeader, Configuration, EventManager, Router, URIUtils, RedirectToLegacyConsole,
    SessionValidator, Constants, query, gotoUrl, logout, store, session, RealmsService,
    ServicesService, MaxIdleTimeLeftStrategy, NavigationHelper, RouteTo, LoginDialog, Queue) => {
    return [{
        startEvent: Constants.EVENT_LOGOUT,
        description: "used to override common logout event",
        override: true,
        processDescription () {
            SessionValidator.stop();

            const logoutSuccess = (response) => {
                store.default.dispatch(session.removeRealm());
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });

                // Use logout response (if there is one) in preference to supplied goto param.
                // Requires any post process url endpoint to redirect to the goto url after it completes
                if (_.has(response, "goto")) {
                    window.location.href = decodeURIComponent(response.goto);
                } else if (gotoUrl.exists()) {
                    window.location.href = gotoUrl.toHref();
                } else {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.loggedOut
                    });
                }
            };

            const logoutFail = () => {
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");

                if (gotoUrl.exists()) {
                    window.location.href = gotoUrl.toHref();
                } else {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                }
            };

            const unvalidatedGotoParam = query.parseParameters(URIUtils.getCurrentFragment()).goto ||
                query.parseParameters(URIUtils.getCurrentQueryString()).goto;
            if (unvalidatedGotoParam) {
                gotoUrl.validateParam(unvalidatedGotoParam).then((validatedUrl) => {
                    gotoUrl.setValidated(validatedUrl);
                    logout.default().then(logoutSuccess, logoutFail);
                }, () => {
                    gotoUrl.remove();
                    logout.default().then(logoutSuccess, logoutFail);
                });
            } else {
                gotoUrl.remove();
                logout.default().then(logoutSuccess, logoutFail);
            }
        }
    }, {
        startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
        description: "",
        processDescription () {
            const subRealm = Configuration.globalData.auth.subRealm || "/";
            window.location.href = `${Constants.context}/realm/RMRealm?RMRealm.tblDataActionHref=${
                encodeURIComponent(subRealm)
            }`;
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_FEDERATION,
        description: "",
        processDescription () {
            RedirectToLegacyConsole.global.federation();
        }
    }, {
        startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
        description: "",
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
        description: "",
        processDescription () {
            Footer.render();
            LoginHeader.render();
        }
    }, {
        startEvent: Constants.EVENT_AUTHENTICATED,
        description: "",
        processDescription () {
            const queueName = "loginDialogAuthCallbacks";
            let authenticatedCallback;

            if (Configuration.globalData[queueName]) {
                authenticatedCallback = Configuration.globalData[queueName].remove();
            }

            if (Configuration.loggedUser && Configuration.loggedUser.hasRole("ui-realm-admin")) {
                RealmsService.realms.all().then(NavigationHelper.populateRealmsDropdown);
                const suppressError = { errorsHandlers : { "Forbidden": { status: 403 } } };
                ServicesService.instance.get("rest", suppressError)
                    .then(NavigationHelper.hideAPILinksOnAPIDescriptionsDisabled);
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
        description: "",
        override: true,
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
        description: "",
        override: true,
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
                    return logout.default().then(() => {
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

                    // only render the LoginDialog if it has an empty callback queue
                    if (!Configuration.globalData[queueName].peek()) {
                        LoginDialog.render();
                    }
                    if (event.authenticatedCallback) {
                        Configuration.globalData[queueName].add(event.authenticatedCallback);
                    }
                }
            }
        }
    }];
});
