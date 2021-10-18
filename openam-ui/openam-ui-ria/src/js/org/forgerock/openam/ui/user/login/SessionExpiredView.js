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

import i18next from "i18next";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import RESTLoginHelper from "org/forgerock/openam/ui/user/login/RESTLoginHelper";
import removeOAuth2Goto from "org/forgerock/openam/ui/user/login/removeOAuth2Goto";
import navigateThenRefresh from "org/forgerock/openam/ui/user/login/navigateThenRefresh";

const SessionExpiredView = AbstractView.extend({
    template: "openam/ReturnToLoginTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    data: {},
    events: {
        "click [data-return-to-login-page]" : navigateThenRefresh
    },
    render () {
        const successfulLoginUrlParams = removeOAuth2Goto(RESTLoginHelper.getSuccessfulLoginUrlParams());
        RESTLoginHelper.removeSuccessfulLoginUrlParams();

        /*
        The RESTLoginHelper.filterUrlParams returns a filtered list of the parameters from the value set within the
        Configuration.globalData.auth.fullLoginURL which is populated by the server upon successful login.
        Once the session has ended we need to manually remove the fullLoginURL as it is no longer valid and can
        cause problems to subsequent failed login requests - i.e ones which do not override the current value.
        FIXME: Remove all session specific properties from the globalData object.
        */
        this.data.params = RESTLoginHelper.filterUrlParams(successfulLoginUrlParams);
        this.data.title = i18next.t("templates.user.SessionExpiredTemplate.sessionExpired");
        this.parentRender();
    }
});

export default new SessionExpiredView();