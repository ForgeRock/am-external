/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationDevicesView from "org/forgerock/openam/ui/user/dashboard/views/AuthenticationDevicesView";
import MyApplicationsView from "org/forgerock/openam/ui/user/dashboard/views/MyApplicationsView";
import OAuthTokensView from "org/forgerock/openam/ui/user/dashboard/views/OAuthTokensView";
import TrustedDevicesView from "org/forgerock/openam/ui/user/dashboard/views/TrustedDevicesView";

class Dashboard extends AbstractView {
    constructor () {
        super();
        this.template = "user/dashboard/DashboardTemplate";
    }
    render () {
        this.parentRender(() => {
            MyApplicationsView.render();
            TrustedDevicesView.render();
            OAuthTokensView.render();
            AuthenticationDevicesView.render();
        });
    }
}

export default new Dashboard();
