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
 * Copyright 2011-2017 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router"
], ($, _, Constants, EventManager, Router) => {
    return [{
        startEvent: Constants.EVENT_LOGOUT,
        description: "used to override common logout event",
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/openam/ui/common/sessions/SessionValidator",
            "store/modules/local/session",
            "store/index",
            "org/forgerock/openam/ui/user/login/gotoUrl",
            "org/forgerock/openam/ui/user/login/logout",
            "org/forgerock/openam/ui/common/util/uri/query",
            "org/forgerock/commons/ui/common/util/URIUtils"
        ],
        processDescription (event, Router, Configuration, SessionValidator, session, store, gotoUrl, logout,
            query, URIUtils) {
            SessionValidator.stop();

            const logoutSuccess = (response) => {
                store.default.dispatch(session.removeRealm());
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                delete Configuration.gotoURL;

                if (gotoUrl.exists()) {
                    window.location.href = gotoUrl.toHref();
                } else if (_.has(response, "goto")) {
                    // The response.goto comes from the "Logout Location" that can be configured in SAML2
                    window.location.href = decodeURIComponent(response.goto);
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
        startEvent: Constants.EVENT_INVALID_REALM,
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/commons/ui/common/main/Configuration"
        ],
        processDescription (event, router, conf) {
            if (event.error.responseJSON.message.indexOf("Invalid realm") > -1) {
                if (conf.baseTemplate) {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                } else {
                    router.navigate("login", { trigger: true });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                }
            }
        }
    }, {
        startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/common/main/Configuration"
        ],
        processDescription (event, conf) {
            const subRealm = conf.globalData.auth.subRealm || "/";
            window.location.href = `${Constants.context}/realm/RMRealm?RMRealm.tblDataActionHref=${
                encodeURIComponent(subRealm)
            }`;
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_FEDERATION,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.global.federation();
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_DATASTORE,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.realm.dataStores(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_PRIVILEGES,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.realm.privileges(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_SUBJECTS,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.realm.subjects(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_AGENTS_OAUTH20,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.agents.oauth20.list(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_AGENTS_RCS,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.agents.rcs(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_AGENTS,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.agents.web(event);
        }
    }, {
        startEvent: Constants.EVENT_REDIRECT_TO_JATO_STS,
        description: "",
        dependencies: [
            "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
        ],
        processDescription (event, RedirectToLegacyConsole) {
            RedirectToLegacyConsole.realm.sts(event);
        }
    }, {
        startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/commons/ui/common/main/Router"
        ],
        processDescription (event, Configuration, Router) {
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
        dependencies: [
            "Footer",
            "org/forgerock/commons/ui/common/components/LoginHeader"
        ],
        processDescription (event, Footer, LoginHeader) {
            Footer.render();
            LoginHeader.render();
        }
    }, {
        startEvent: Constants.EVENT_AUTHENTICATED,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/openam/ui/admin/services/global/RealmsService",
            "org/forgerock/openam/ui/admin/services/global/ServicesService",
            "org/forgerock/openam/ui/common/sessions/SessionValidator",
            "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy",
            "org/forgerock/openam/ui/common/util/NavigationHelper"
        ],
        processDescription (event, Configuration, RealmsService, ServicesService, SessionValidator,
            MaxIdleTimeLeftStrategy, NavigationHelper) {
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
        dependencies: [
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/openam/ui/common/RouteTo"
        ],
        processDescription (event, Configuration, RouteTo) {
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
        dependencies: [
            "LoginDialog",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/commons/ui/common/util/Queue",
            "org/forgerock/openam/ui/user/login/logout"
        ],
        processDescription (event, LoginDialog, Configuration, Queue, logout) {
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
