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

import { has } from "lodash";

import { validateParam as validateGotoParam } from "org/forgerock/openam/ui/user/login/gotoUrl";
import { parseParameters } from "org/forgerock/openam/ui/common/util/uri/query";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import logout from "org/forgerock/openam/ui/user/login/logout";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import RESTLoginHelper from "org/forgerock/openam/ui/user/login/RESTLoginHelper";
import removeOAuth2Goto from "org/forgerock/openam/ui/user/login/removeOAuth2Goto";
import Router from "org/forgerock/commons/ui/common/main/Router";
import navigateThenRefresh from "org/forgerock/openam/ui/user/login/navigateThenRefresh";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const LogoutView = AbstractView.extend({
    template: "openam/RestLogoutTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    data: {},
    events: {
        "click [data-return-to-login-page]" : navigateThenRefresh
    },
    render () {
        let validatedGotoUrl;
        const logoutSuccess = (response) => {
            // Use logout response (if there is one) in preference to supplied goto param.
            // Requires any post process url endpoint to redirect to the goto url after it completes
            if (has(response, "goto")) {
                window.location.href = decodeURIComponent(response.goto);
            } else if (validatedGotoUrl) {
                window.location.href = validatedGotoUrl;
            } else {
                this.data.loggedOut = true;
                this.parentRender();
            }
        };

        const logoutFail = (response) => {
            Messages.addMessage({ type: Messages.TYPE_DANGER, response });

            if (validatedGotoUrl) {
                window.location.href = validatedGotoUrl;
            } else {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, { route: Router.configuration.routes.login });
            }
        };
        /*
        The RESTLoginHelper.filterUrlParams returns a filtered list of the parameters from the value set within the
        Configuration.globalData.auth.fullLoginURL which is populated by the server upon successful login.
        Once the session has ended we need to manually remove the fullLoginURL as it is no longer valid and can
        cause problems to subsequent failed login requests - i.e ones which do not override the current value.
        FIXME: Remove all session specific properties from the globalData object.
        */
        const successfulLoginUrlParams = removeOAuth2Goto(RESTLoginHelper.getSuccessfulLoginUrlParams());
        RESTLoginHelper.removeSuccessfulLoginUrlParams();
        this.data.params = RESTLoginHelper.filterUrlParams(successfulLoginUrlParams);
        this.data.loggedOut = false;
        SessionValidator.stop();
        const suppressSpinner = { suppressSpinner: true };

        this.parentRender(() => {
            const unvalidatedGoto = parseParameters(URIUtils.getCurrentFragment()).goto ||
            parseParameters(URIUtils.getCurrentQueryString()).goto;
            if (unvalidatedGoto) {
                validateGotoParam(unvalidatedGoto).then((validatedUrl) => {
                    validatedGotoUrl = validatedUrl;
                    logout(suppressSpinner).then(logoutSuccess, logoutFail);
                }, () => {
                    logout(suppressSpinner).then(logoutSuccess, logoutFail);
                });
            } else {
                logout(suppressSpinner).then(logoutSuccess, logoutFail);
            }
        });
    }
});

export default new LogoutView();
