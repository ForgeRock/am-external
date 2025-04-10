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

import { getSessionInfo } from "org/forgerock/openam/ui/user/services/SessionService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import getCurrentFragmentParamString from "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString";
import isRealmChanged from "org/forgerock/openam/ui/common/util/isRealmChanged";
import logout from "org/forgerock/openam/ui/user/login/logout";
import Router from "org/forgerock/commons/ui/common/main/Router";

function gotoLoginWithParams (args) {
    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, { args, route: Router.configuration.routes.login });
}

function removeUserAndGotoLogin (args) {
    Configuration.setProperty("loggedUser", null);
    gotoLoginWithParams(args);
}

const SwitchRealmView = AbstractView.extend({
    template: "openam/SwitchRealmsTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    data: {},
    events: {
        "click [data-switch-realms]" : "onSwitchRealmsHandler"
    },
    partials: {
        "alerts/_Alert": "alerts/_Alert"
    },
    render () {
        this.data.fragmentParamString = getCurrentFragmentParamString();
        this.data.args = [this.data.fragmentParamString];

        if (isRealmChanged()) {
            getSessionInfo().then(
                () => this.parentRender(),
                () => removeUserAndGotoLogin(this.data.args)
            );
        } else {
            removeUserAndGotoLogin(this.data.args); // Realm not changed, but params may have
        }
    },
    onSwitchRealmsHandler (event) {
        event.preventDefault();
        const routeToLogin = () => {
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                args : this.data.args,
                route: Router.configuration.routes.login
            });
        };
        logout().then(routeToLogin, routeToLogin);
    }
});

export default new SwitchRealmView();
