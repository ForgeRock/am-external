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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * @module org/forgerock/openam/ui/user/login/logout
 */

import { logout as serviceLogout } from "org/forgerock/openam/ui/user/services/SessionService";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import removeLocalUserData from "org/forgerock/openam/ui/user/login/removeLocalUserData";
import Router from "org/forgerock/commons/ui/common/main/Router";

/**
 * @see https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/status
 */
const isNetworkError = (status) => status === 0;

const logout = () => {
    return serviceLogout().then((response) => {
        removeLocalUserData();
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
        return response;
    }, (error) => {
        if (isNetworkError(error.status)) {
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: Router.configuration.routes.serviceUnavailable
            });
        } else {
            removeLocalUserData();
            return error;
        }
    });
};

export default logout;
