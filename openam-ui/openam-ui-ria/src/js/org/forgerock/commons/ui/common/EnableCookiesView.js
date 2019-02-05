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
 * Copyright 2011-2018 ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";

const EnableCookiesView = AbstractView.extend({
    template: "common/EnableCookiesTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    render () {
        if (CookieHelper.cookiesEnabled()) {
            location.href = "#login/";
        } else {
            this.parentRender();
        }
    }
});

export default new EnableCookiesView();