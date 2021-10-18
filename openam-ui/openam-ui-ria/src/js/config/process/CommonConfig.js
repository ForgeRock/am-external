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
 * Copyright 2011-2021 ForgeRock AS.
 */

import $ from "jquery";

import { changeView } from "org/forgerock/commons/ui/common/main/ViewManager";
import { init as i18nInit } from "org/forgerock/commons/ui/common/main/i18n/manager";
import { hideAPILinksOnAPIDescriptionsDisabled, populateRealmsDropdown } from
    "org/forgerock/openam/ui/common/util/NavigationHelper";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import ErrorsHandler from "org/forgerock/commons/ui/common/main/ErrorsHandler";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Footer from "Footer";
import KBAView from "org/forgerock/commons/ui/user/anonymousProcess/KBAView";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Navigation from "org/forgerock/commons/ui/common/components/Navigation";
import RealmsService from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import Router from "org/forgerock/commons/ui/common/main/Router";
import routes from "config/routes";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";
import SessionManager from "org/forgerock/commons/ui/common/main/SessionManager";
import SiteConfigurator from "org/forgerock/commons/ui/common/SiteConfigurator";
import SiteConfigurationService from "org/forgerock/openam/ui/common/services/SiteConfigurationService";
import SpinnerManager from "org/forgerock/commons/ui/common/main/SpinnerManager";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const CommonConfig = [{
    startEvent: Constants.EVENT_APP_INITIALIZED,
    processDescription () {
        const postSessionCheck = async function () {
            await UIUtils.preloadInitialPartials();
            Router.init(routes);
            if (!CookieHelper.cookiesEnabled()) {
                location.href = "#enableCookies/";
            }

            if (Configuration.loggedUser && Configuration.loggedUser.hasRole("ui-realm-admin")) {
                RealmsService.realms.all().then(populateRealmsDropdown);
                const suppressError = { errorsHandlers : { "Forbidden": { status: 403 } } };
                ServicesService.instance.get("rest", suppressError).then(hideAPILinksOnAPIDescriptionsDisabled);
            }
        };

        i18nInit().then(() => {
            SessionManager.getLoggedUser((user) => {
                Configuration.setProperty("loggedUser", user);
                // WARNING - do not use the promise returned from sendEvent as an example for using this system
                // TODO - replace with simplified event system as per CUI-110
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false })
                    .then(postSessionCheck);
            }, (error) => {
                if (error && error.status === 503) {
                    window.location.pathname = `${window.location.pathname}503.html`;
                    return;
                }
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true })
                    .then(postSessionCheck);
            });
            const lang = Configuration.globalData.lang || "en";
            $("html").attr("lang", lang);
        });
    }
}, {
    startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
    processDescription () {
        LoginHeader.render();
        Navigation.init();
        Footer.render();
    }
}, {
    startEvent: Constants.EVENT_READ_CONFIGURATION_REQUEST,
    processDescription () {
        if (!Configuration.globalData) {
            Configuration.setProperty("globalData", {});
            Configuration.globalData.auth = {};
        }

        if (SiteConfigurator.initialized === false) {
            SiteConfigurator.initialized = true;

            SiteConfigurationService.getConfiguration((config) => {
                SiteConfigurator.processConfiguration(config);
                EventManager.sendEvent(Constants.EVENT_APP_INITIALIZED);
            }, () => {
                SiteConfigurator.processConfiguration({});
                EventManager.sendEvent(Constants.EVENT_APP_INITIALIZED);
            });
        }
    }
}, {
    startEvent: Constants.EVENT_AUTHENTICATION_DATA_CHANGED,
    processDescription (event) {
        const defaultHeaders = ServiceInvoker.configuration.defaultHeaders;
        if (event.anonymousMode) {
            defaultHeaders[Constants.HEADER_PARAM_PASSWORD] = Constants.ANONYMOUS_PASSWORD;
            defaultHeaders[Constants.HEADER_PARAM_USERNAME] = Constants.ANONYMOUS_USERNAME;
            defaultHeaders[Constants.HEADER_PARAM_NO_SESSION] = true;

            Configuration.setProperty("loggedUser", null);
            Configuration.setProperty("gotoFragement", null);
            Navigation.reload();
        } else {
            delete Configuration.globalData.authorizationFailurePending;
            delete defaultHeaders[Constants.HEADER_PARAM_PASSWORD];
            delete defaultHeaders[Constants.HEADER_PARAM_USERNAME];
            delete defaultHeaders[Constants.HEADER_PARAM_NO_SESSION];

            EventManager.sendEvent(Constants.EVENT_AUTHENTICATED);
        }
    }
}, {
    startEvent: Constants.EVENT_UNAUTHENTICATED,
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
}, {
    startEvent: Constants.EVENT_REST_CALL_ERROR,
    processDescription (event) {
        ErrorsHandler.handleError(event.data, event.errorsHandlers);
        SpinnerManager.hideSpinner();
    }
}, {
    startEvent: Constants.EVENT_START_REST_CALL,
    processDescription (event) {
        if (!event.suppressSpinner) {
            SpinnerManager.showSpinner();
        }
    }
}, {
    startEvent: Constants.EVENT_END_REST_CALL,
    processDescription () {
        SpinnerManager.hideSpinner();
    }
}, {
    startEvent: Constants.EVENT_CHANGE_VIEW,
    processDescription (event) {
        let params = event.args;
        const route = event.route;
        const callback = event.callback;
        const fromRouter = event.fromRouter;

        Router.checkRole(route);

        if (Configuration.hasRESTLoginDialog) {
            return;
        }

        return route.view().then(unwrapDefaultExport).then((view) => {
            view.route = route;

            params = params || route.defaults;
            Configuration.setProperty("baseView", "");
            Configuration.setProperty("baseViewArgs", "");

            SpinnerManager.hideSpinner(10);
            if (!fromRouter) {
                Router.routeTo(route, { trigger: true, args: params });
                return;
            }

            const promise = $.Deferred();
            changeView(route.view, params, () => {
                if (callback) {
                    callback();
                }
                promise.resolve(view);
            }, route.forceUpdate);

            Navigation.reload();
            return promise;
        });
    }
}, {
    startEvent: Constants.EVENT_SERVICE_UNAVAILABLE,
    processDescription () {
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "serviceUnavailable");
    }
}, {
    startEvent: Constants.ROUTE_REQUEST,
    processDescription (event) {
        const route = Router.configuration.routes[event.routeName];

        // trigger defaults to true
        if (event.trigger === undefined) {
            event.trigger = true;
        }

        Router.routeTo(route, event);
        Navigation.reload();
    }
}, {
    startEvent: Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
    processDescription (event) {
        Messages.messages.displayMessageFromConfig(event);
    }
}, {
    startEvent: Constants.EVENT_SELECT_KBA_QUESTION,
    processDescription () {
        KBAView.changeQuestion();
    }
}, {
    startEvent: Constants.EVENT_DELETE_KBA_QUESTION,
    processDescription (event) {
        KBAView.deleteQuestion(event.viewId);
    }
}];

export default CommonConfig;
