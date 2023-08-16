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

import { updateSessionInfo } from "org/forgerock/openam/ui/user/services/SessionService";
import UserModel from "org/forgerock/openam/ui/user/UserModel";

const RESTLoginHelper = {};

RESTLoginHelper.getLoggedUser = function (successCallback, errorCallback) {
    return updateSessionInfo().then((data) => {
        return UserModel.fetchById(data.username).then(successCallback);
    }, (response) => {
        if (_.get(response, "responseJSON.code") === 404) {
            errorCallback("loggedIn");
        } else {
            errorCallback();
        }
    });
};

export default RESTLoginHelper;