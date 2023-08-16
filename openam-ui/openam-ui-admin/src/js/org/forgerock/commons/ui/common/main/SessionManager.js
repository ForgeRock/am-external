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
 * Copyright 2011-2019 ForgeRock AS.
 */

import _ from "lodash";

import ModuleLoader from "org/forgerock/commons/ui/common/util/ModuleLoader";
import RESTLoginHelper from "org/forgerock/openam/ui/user/login/RESTLoginHelper";

const SessionManager = {};

SessionManager.getLoggedUser = function (successCallback, errorCallback) {
    return ModuleLoader.promiseWrapper(_.bind(RESTLoginHelper.getLoggedUser, RESTLoginHelper), {
        success: successCallback,
        error: errorCallback
    });
};

export default SessionManager;