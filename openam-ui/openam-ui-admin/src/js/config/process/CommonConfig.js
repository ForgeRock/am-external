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

import { t } from "i18next";
import $ from "jquery";

import { changeView } from "org/forgerock/commons/ui/common/main/ViewManager";
import { init as i18nInit } from "org/forgerock/commons/ui/common/main/i18n/manager";
import { getAll as getAllRealms } from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import {
    hideAPILinksOnAPIDescriptionsDisabled,
    populateRealmsDropdown
} from "org/forgerock/openam/ui/common/util/NavigationHelper";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Footer from "org/forgerock/openam/ui/common/components/Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Navigation from "org/forgerock/commons/ui/common/components/Navigation";
import redirectToUserLoginWithGoto from "org/forgerock/openam/ui/common/redirectToUser/loginWithGoto";
import Router from "org/forgerock/commons/ui/common/main/Router";
import routes from "config/routes";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";
import SessionManager from "org/forgerock/commons/ui/common/main/SessionManager";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import SiteConfigurator from "org/forgerock/commons/ui/common/SiteConfigurator";
import SpinnerManager from "org/forgerock/commons/ui/common/main/SpinnerManager";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";

const CommonConfig = [{
    startEvent: Constants.EVENT_APP_INITIALIZED,
    processDescription () {
        Promise.all([i18nInit(), UIUtils.preloadInitialPartials()]).then(() => {
            SessionManager.getLoggedUser((user) => {
                Configuration.setProperty("loggedUser", user);
                ServiceInvoker.removeAnonymousDefaultHeaders();
                Router.init(routes);
                if (user.uiroles.includes("ui-realm-admin")) {
                    getAllRealms().then(populateRealmsDropdown);
                    const suppressError = { errorsHandlers : { "Forbidden": { status: 403 } } };
                    ServicesService.instance.get("rest", suppressError).then(hideAPILinksOnAPIDescriptionsDisabled);
                }

                if (Configuration.globalData.xuiUserSessionValidationEnabled) {
                    SessionValidator.start(MaxIdleTimeLeftStrategy);
                }

                // Auditor role logic (removes all buttons and disables all inputs)
                if (Configuration.loggedUser.get("roles").includes("ui-auditor")) {
                    const bodyTag = document.getElementsByTagName("body")[0];
                    const observer = new MutationObserver(() => {
                        const cssSelectors = [
                            'input:not([placeholder^="Search"]):not([placeholder^="Filter"]):not([id="findAUser"])',
                            "textarea"
                        ];
                        const inputs = document.querySelectorAll(cssSelectors.join(","));
                        inputs.forEach((input) => {
                            input.disabled = true;
                        });
                    });

                    bodyTag.classList.add("Auditor");
                    observer.observe(document.body, { childList: true, subtree: true });
                }
            }, () => {
                ServiceInvoker.setAnonymousDefaultHeaders();
                Router.init(routes);
                if (!CookieHelper.cookiesEnabled()) {
                    location.href = "#enableCookies/";
                }
            });
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
    startEvent: Constants.EVENT_UNAUTHENTICATED,
    processDescription () {
        redirectToUserLoginWithGoto();
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
                redirectToUserLoginWithGoto();
            }
        }

        return route.view().then(unwrapDefaultExport).then((view) => {
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

                changeView(route.view, params, () => {
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
}, {
    startEvent: Constants.EVENT_SERVICE_UNAVAILABLE,
    processDescription () {
        Messages.addMessage({
            message: t("config.messages.CommonMessages.serviceUnavailable"),
            type: Messages.TYPE_DANGER
        });
    }
}, {
    startEvent: Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
    processDescription (event) {
        // Deprecated - use Messages.addMessage directly.
        Messages.messages.displayMessageFromConfig(event);
    }
}];

export default CommonConfig;
