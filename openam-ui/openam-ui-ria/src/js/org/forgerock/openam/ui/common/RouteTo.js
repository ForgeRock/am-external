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
 * Copyright 2015-2019 ForgeRock AS.
 */

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import logout from "org/forgerock/openam/ui/user/login/logout";
import Router from "org/forgerock/commons/ui/common/main/Router";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
  * Provides functions to navigate the application to commonly required routes.
  *
  * @module org/forgerock/openam/ui/common/RouteTo
  */

const RouteTo = {
    setGotoFragment () {
        var fragment = URIUtils.getCurrentFragment();
        if (!Configuration.gotoFragment && !fragment.match(Router.configuration.routes.login.url)) {
            Configuration.setProperty("gotoFragment", `#${fragment}`);
        }
    },
    forbiddenPage () {
        delete Configuration.globalData.authorizationFailurePending;
        return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
            route: Router.configuration.routes.forbidden,
            fromRouter: true
        });
    },
    forbiddenError () {
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
    },
    logout () {
        RouteTo.setGotoFragment();

        const routeToLogin = () => {
            Router.routeTo(Router.configuration.routes.login, { trigger: true });
        };

        return logout().then(routeToLogin, routeToLogin);
    },
    loginDialog () {
        return EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
    }
};

export default RouteTo;
