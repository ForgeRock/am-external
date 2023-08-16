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

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationDevicesView from
    "org/forgerock/openam/ui/user/dashboard/views/authenticationDevices/AuthenticationDevicesView";
import ApplicationsView from "org/forgerock/openam/ui/user/dashboard/views/applications/ApplicationsView";
import OAuthTokensView from "org/forgerock/openam/ui/user/dashboard/views/oAuthTokens/OAuthTokensView";
import TrustedDevicesView from "org/forgerock/openam/ui/user/dashboard/views/trustedDevices/TrustedDevicesView";

class Dashboard extends AbstractView {
    constructor () {
        super();
        this.template = "user/dashboard/DashboardTemplate";
    }
    render () {
        this.parentRender(() => {
            ApplicationsView.render();
            TrustedDevicesView.render();
            OAuthTokensView.render();
            AuthenticationDevicesView.render();
        });
    }
}

export default new Dashboard();