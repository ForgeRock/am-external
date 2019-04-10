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
 * Copyright 2015-2018 ForgeRock AS.
 */

import { assign, get } from "lodash";

import AnonymousProcessView from "org/forgerock/openam/ui/user/anonymousProcess/AnonymousProcessView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import KBAView from "org/forgerock/commons/ui/user/anonymousProcess/KBAView";
import RESTLoginView from "org/forgerock/openam/ui/user/login/RESTLoginView";
import SelfRegistrationView from "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView";

function shouldRouteToLoginView (response, destination) {
    return response.tag === "end" && destination === "login";
}

function shouldAutoLogin (response, destination) {
    return response.tag === "end" && destination === "auto-login";
}

function AMSelfRegistrationView () { }

AMSelfRegistrationView.prototype = SelfRegistrationView;
AMSelfRegistrationView.prototype.endpoint = Constants.SELF_SERVICE_REGISTER;

assign(AMSelfRegistrationView.prototype, AnonymousProcessView.prototype);

AMSelfRegistrationView.prototype.renderProcessState = function (response) {
    const destination = get(Configuration, "globalData.successfulUserRegistrationDestination");
    const realm = get(Configuration, "globalData.realm", "");

    if (shouldAutoLogin(response, destination)) {
        RESTLoginView.handleExistingSession(response.additions);
    } else if (shouldRouteToLoginView(response, destination)) {
        window.location.href = `#login${realm}`;
    } else {
        AnonymousProcessView.prototype.renderProcessState.call(this, response).then(() => {
            if (response.type === "kbaSecurityAnswerDefinitionStage" && response.tag === "initial") {
                KBAView.render(response.requirements.properties.kba);
            }
        });
    }
};

export default new AMSelfRegistrationView();
