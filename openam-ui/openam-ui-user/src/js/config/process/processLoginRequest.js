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
 * Copyright 2018-2023 ForgeRock AS.
 */

import $ from "jquery";

import { exists as hasValidGotoUrl, toHref as gotoUrlToHref } from "org/forgerock/openam/ui/user/login/gotoUrl";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import SessionManager from "org/forgerock/commons/ui/common/main/SessionManager";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";

const processLoginRequest = (event) => {
    const promise = $.Deferred();
    const submitContent = (event && event.submitContent) || event || {};

    SessionManager.login(submitContent, (user, remainingSessionTime) => {
        if (hasValidGotoUrl()) {
            window.location.href = gotoUrlToHref();
            return;
        }

        Configuration.setProperty("loggedUser", user);
        ServiceInvoker.removeAnonymousDefaultHeaders();
        if (Configuration.globalData.xuiUserSessionValidationEnabled) {
            SessionValidator.start(MaxIdleTimeLeftStrategy, remainingSessionTime);
        }
        EventManager.sendEvent(Constants.EVENT_HANDLE_DEFAULT_ROUTE);

        promise.resolve(user);
    }, (reason) => {
        reason = reason ? reason : "authenticationFailed";
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, reason);

        if (event && event.failureCallback) {
            event.failureCallback(reason);
        }

        promise.reject(reason);
    });

    return promise;
};

export default processLoginRequest;
