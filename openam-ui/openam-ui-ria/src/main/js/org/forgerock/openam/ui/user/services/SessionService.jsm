/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";

import { addRealm } from "store/modules/local/session";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import store from "store/index";
import { exists as gotoExists, get as getGoto } from "org/forgerock/openam/ui/user/login/gotoUrl";

const ONE_SECOND_IN_MILLISECONDS = 1000;

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json/sessions`);
const getSessionInfo = (options) => {
    return obj.serviceCall(_.merge({
        url: "?_action=getSessionInfo",
        type: "POST",
        data: {},
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=2.0"
        }
    }, options));
};

export const getTimeLeft = (token) => {
    return getSessionInfo(token, { suppressSpinner: true }).then((sessionInfo) => {
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
    const options = { errorsHandlers : { "Unauthorized": { status: 401 } } };

    return getSessionInfo(options).then((response) => {
        store.dispatch(addRealm(response.realm));
        return response;
    });
};

export const isSessionValid = () => getSessionInfo();

export const logout = () => {
    const paramString = gotoExists() ? `&goto=${getGoto()}` : "";
    return obj.serviceCall({
        url: `?_action=logout${paramString}`,
        type: "POST",
        data: {},
        headers: {
            "Accept-API-Version": "protocol=1.0,resource=2.0"
        },
        errorsHandlers: {
            "Bad Request": { status: 400 },
            "Unauthorized": { status: 401 }
        }
    });
};
