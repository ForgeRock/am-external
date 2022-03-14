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

import { t } from "i18next";
import $ from "jquery";

import { changeView } from "org/forgerock/commons/ui/common/main/ViewManager";
import { init as i18nInit } from "org/forgerock/commons/ui/common/main/i18n/manager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Footer from "org/forgerock/openam/ui/common/components/Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Navigation from "org/forgerock/commons/ui/common/components/Navigation";
import Router from "org/forgerock/commons/ui/common/main/Router";
import routes from "config/routes";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import SessionManager from "org/forgerock/commons/ui/common/main/SessionManager";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import SiteConfigurator from "org/forgerock/commons/ui/common/SiteConfigurator";
import SpinnerManager from "org/forgerock/commons/ui/common/main/SpinnerManager";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import SiteConfigurationService from "org/forgerock/openam/ui/common/services/SiteConfigurationService";

const CommonConfig = [{
    startEvent: Constants.EVENT_APP_INITIALIZED,
    processDescription () {
        Promise.all([i18nInit(), UIUtils.preloadInitialPartials()]).then(() => {
            SessionManager.getLoggedUser((user, remainingSessionTime) => {
                Configuration.setProperty("loggedUser", user);
                ServiceInvoker.removeAnonymousDefaultHeaders();
                Router.init(routes);
                if (Configuration.globalData.xuiUserSessionValidationEnabled) {
                    SessionValidator.start(MaxIdleTimeLeftStrategy, remainingSessionTime);
                }
            }, (error) => {
                if (error && error.status === 503) {
                    window.location.pathname = `${window.location.pathname}503.html`;
                    return;
                }
                ServiceInvoker.setAnonymousDefaultHeaders();
                Router.init(routes);
                if (!CookieHelper.cookiesEnabled()) {
                    location.href = "#enableCookies/";
                }
            });
            const lang = Configuration.globalData.lang || "en";
            $("html").attr("lang", lang);
        });
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
    startEvent: Constants.EVENT_CHANGE_BASE_VIEW,
    processDescription () {
        LoginHeader.render();
        Navigation.init();
        Footer.render();
    }
}, {
    startEvent: Constants.EVENT_UNAUTHENTICATED,
    processDescription () {
        ServiceInvoker.setAnonymousDefaultHeaders();
        Configuration.setProperty("loggedUser", null);
        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
            route: Router.configuration.routes.login
        });
    }
}, {
    startEvent: Constants.EVENT_CHANGE_VIEW,
    processDescription (event) {
        let params = event.args;
        const route = event.route;
        const callback = event.callback;
        const fromRouter = event.fromRouter;

        if (!Router.isRoleAuthorized(route.role)) {
            if (Configuration.loggedUser) {
                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, { fromRouter: true });
            } else {
                EventManager.sendEvent(Constants.EVENT_UNAUTHENTICATED, { fromRouter: true });
            }
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
        Messages.addMessage({
            message: t("config.messages.CommonMessages.serviceUnavailable"),
            type: Messages.TYPE_DANGER
        });
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
        // Deprecated - use Messages.addMessage directly.
        Messages.messages.displayMessageFromConfig(event);
    }
}];

export default CommonConfig;
