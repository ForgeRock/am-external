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
 * Copyright 2014-2019 ForgeRock AS.
 */

import _ from "lodash";

import { addRealm } from "store/modules/local/session";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import store from "store";

const ONE_SECOND_IN_MILLISECONDS = 1000;

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json/sessions`);
const getSessionInfo = (options) => {
    return obj.serviceCall(_.merge({
        errorsHandlers : { "Unauthorized": { status: 401 } },
        url: "?_action=getSessionInfo",
        type: "POST",
        data: {},
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=2.0"
        },
        suppressSpinner: true
    }, options));
};

export const getTimeLeft = () => {
    return getSessionInfo().then((sessionInfo) => {
        const differenceInSeconds = (date) => {
            const differenceInMilliseconds = Date.parse(date) - Date.now();
            return Math.round(differenceInMilliseconds / ONE_SECOND_IN_MILLISECONDS);
        };
        const idleExpiration = differenceInSeconds(sessionInfo.maxIdleExpirationTime);
        const maxExpiration = differenceInSeconds(sessionInfo.maxSessionExpirationTime);
        return _.min([idleExpiration, maxExpiration]);
    });
};

export const updateSessionInfo = () => {
    return getSessionInfo().then((response) => {
        store.dispatch(addRealm(response.realm));
        return response;
    });
};
