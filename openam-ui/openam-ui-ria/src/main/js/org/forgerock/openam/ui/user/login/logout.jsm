/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/user/login/logout
 */

import $ from "jquery";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import { logout as serviceLogout } from "org/forgerock/openam/ui/user/services/SessionService";

const logout = () => {
    Configuration.setProperty("loggedUser", null);

    return serviceLogout().then(null, () => {
        return $.Deferred().resolve();
    });
};

export default logout;
