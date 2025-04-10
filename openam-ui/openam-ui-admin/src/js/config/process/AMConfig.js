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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import Footer from "org/forgerock/openam/ui/common/components/Footer";
import LoginHeader from "org/forgerock/commons/ui/common/components/LoginHeader";
import redirectToUserLoginWithGoto from "org/forgerock/openam/ui/common/redirectToUser/loginWithGoto";
import redirectToUserLogout from "org/forgerock/openam/ui/common/redirectToUser/logout";
import Router from "org/forgerock/commons/ui/common/main/Router";
import RouteTo from "org/forgerock/openam/ui/common/RouteTo";

export default [{
    startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
    processDescription () {
        if (!Configuration.loggedUser) {
            redirectToUserLoginWithGoto();
        } else if (_.includes(Configuration.loggedUser.uiroles, "ui-realm-admin")) {
            Router.routeTo(Router.configuration.routes.realms, { args: [], replace: true, trigger: true });
        } else {
            RouteTo.forbiddenPage();
        }
    }
}, {
    startEvent: Constants.EVENT_THEME_CHANGED,
    processDescription () {
        Footer.render();
        LoginHeader.render();
    }
}, {
    startEvent: Constants.EVENT_UNAUTHORIZED,
    processDescription (event) {
        if (!Configuration.loggedUser) {
            redirectToUserLogout();
        } else if (_.get(event, "fromRouter")) {
            // 403 route change
            RouteTo.forbiddenPage();
        } else {
            // 403 rest call
            RouteTo.forbiddenError();
        }
    }
}];
