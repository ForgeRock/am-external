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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/i18n/manager",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/commons/ui/common/components/LoginHeader",
    "Footer",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/ErrorsHandler",
    "org/forgerock/commons/ui/common/SiteConfigurator",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "LoginDialog",
    "org/forgerock/commons/ui/common/util/Queue",
    "org/forgerock/commons/ui/user/anonymousProcess/KBAView"
], ($, _, Constants, EventManager, Router, Configuration, UIUtils, CookieHelper, SessionManager,
    i18n, Navigation, LoginHeader, Footer, ServiceInvoker, URIUtils, ViewManager,
    ErrorsHandler, SiteConfigurator, SpinnerManager, Messages, LoginDialog, Queue, KBAView) => {
    const obj = [
        {
            startEvent: Constants.EVENT_APP_INITIALIZED,
            description: "Starting basic components",
            processDescription () {
                const postSessionCheck = function () {
                    UIUtils.preloadInitialPartials();
                    Router.init();
                };

                i18n.init().then(() => {
                    SessionManager.getLoggedUser((user) => {
                        Configuration.setProperty("loggedUser", user);
                        // WARNING - do not use the promise returned from sendEvent as an example for using this system
                        // TODO - replace with simplified event system as per CUI-110
                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false })
                            .then(postSessionCheck);
                    }, () => {
                        if (!CookieHelper.cookiesEnabled()) {
                            location.href = "#enableCookies/";
                        }
                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true })
                            .then(postSessionCheck);
                    });
                });
            }
        },
        {
            startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
            description: "",
            processDescription () {
                LoginHeader.render();
                Navigation.init();
                Footer.render();
            }
        },
        {
            startEvent: Constants.EVENT_AUTHENTICATION_DATA_CHANGED,
            description: "",
            processDescription (event) {
                if (event.anonymousMode) {
                    ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_PASSWORD] =
                        Constants.ANONYMOUS_PASSWORD;
                    ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_USERNAME] =
                        Constants.ANONYMOUS_USERNAME;
                    ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION] = true;

                    Configuration.setProperty("loggedUser", null);
                    Configuration.setProperty("gotoFragement", null);
                    Navigation.reload();
                } else {
                    delete Configuration.globalData.authorizationFailurePending;
                    delete ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_PASSWORD];
                    delete ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_USERNAME];
                    delete ServiceInvoker.configuration.defaultHeaders[Constants.HEADER_PARAM_NO_SESSION];

                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATED);
                }
            }
        },
        {
            startEvent: Constants.EVENT_UNAUTHORIZED,
            description: "",
            processDescription () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
            }
        },
        {
            startEvent: Constants.EVENT_UNAUTHENTICATED,
            description: "",
            processDescription () {
                const fragment = URIUtils.getCurrentFragment();
                if (!Configuration.gotoFragment && !fragment.match(Router.configuration.routes.login.url)) {
                    Configuration.setProperty("gotoFragment", `#${fragment}`);
                }
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                    anonymousMode: true
                });
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.login
                });
            }
        },
        {
            startEvent: Constants.EVENT_REST_CALL_ERROR,
            description: "",
            processDescription (event) {
                ErrorsHandler.handleError(event.data, event.errorsHandlers);
                SpinnerManager.hideSpinner();
            }
        },
        {
            startEvent: Constants.EVENT_START_REST_CALL,
            description: "",
            processDescription (event) {
                if (!event.suppressSpinner) {
                    SpinnerManager.showSpinner();
                }
            }
        },
        {
            startEvent: Constants.EVENT_END_REST_CALL,
            description: "",
            processDescription () {
                SpinnerManager.hideSpinner();
            }
        },
        {
            startEvent: Constants.EVENT_CHANGE_VIEW,
            description: "",
            processDescription (event) {
                let params = event.args;
                const route = event.route;
                const callback = event.callback;
                const fromRouter = event.fromRouter;

                if (!Router.checkRole(route)) {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.unauthorized,
                        fromRouter: true
                    });
                    return;
                }

                if (Configuration.backgroundLogin) {
                    return;
                }

                return route.view().then((view) => {
                    view.route = route;

                    params = params || route.defaults;
                    Configuration.setProperty("baseView", "");
                    Configuration.setProperty("baseViewArgs", "");

                    return SiteConfigurator.configurePage(route, params).then(() => {
                        const promise = $.Deferred();
                        SpinnerManager.hideSpinner(10);
                        if (!fromRouter) {
                            Router.routeTo(route, { trigger: true, args: params });
                        }

                        ViewManager.changeView(route.view, params, () => {
                            if (callback) {
                                callback();
                            }
                            promise.resolve(view);
                        }, route.forceUpdate);

                        Navigation.reload();
                        return promise;
                    });
                });
            }
        },
        {
            startEvent: Constants.EVENT_SERVICE_UNAVAILABLE,
            description: "",
            processDescription () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "serviceUnavailable");
            }
        },
        {
            startEvent: Constants.ROUTE_REQUEST,
            description: "",
            processDescription (event) {
                const route = Router.configuration.routes[event.routeName];

                // trigger defaults to true
                if (event.trigger === undefined) {
                    event.trigger = true;
                } else if (event.trigger === false) {
                    ViewManager.currentView = route.view;
                    ViewManager.currentViewArgs = event.args;
                }

                Router.routeTo(route, event);
                Navigation.reload();
            }
        },
        {
            startEvent: Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
            description: "",
            processDescription (event) {
                Messages.messages.displayMessageFromConfig(event);
            }
        },
        {
            startEvent: Constants.EVENT_SHOW_LOGIN_DIALOG,
            description: "",
            processDescription (event) {
                const queueName = "loginDialogAuthCallbacks";
                if (!Configuration.globalData[queueName]) {
                    Configuration.globalData[queueName] = new Queue();
                }
                // only render the LoginDialog if it has an empty callback queue
                if (!Configuration.globalData[queueName].peek()) {
                    LoginDialog.render({
                        authenticatedCallback () {
                            let callback = Configuration.globalData[queueName].remove();
                            while (callback) {
                                callback();
                                callback = Configuration.globalData[queueName].remove();
                            }
                        }
                    });
                }
                if (event.authenticatedCallback) {
                    Configuration.globalData[queueName].add(event.authenticatedCallback);
                }
            }
        },
        {
            startEvent: Constants.EVENT_LOGOUT,
            description: "",
            processDescription () {
                return SessionManager.logout(() => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                    return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                });
            }
        },
        {
            startEvent: Constants.EVENT_SELECT_KBA_QUESTION,
            description: "",
            processDescription () {
                KBAView.changeQuestion();
            }
        },
        {
            startEvent: Constants.EVENT_DELETE_KBA_QUESTION,
            description: "",
            processDescription (event) {
                KBAView.deleteQuestion(event.viewId);
            }
        }];
    return obj;
});
