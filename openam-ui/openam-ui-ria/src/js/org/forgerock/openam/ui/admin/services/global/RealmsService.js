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
 * Copyright 2016-2018 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/RealmsService
 */

import _ from "lodash";

import { addRealm, removeRealm, setRealms } from "store/modules/remote/realms";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import SMSServiceUtils from "org/forgerock/openam/ui/admin/services/SMSServiceUtils";
import store from "store";

const RealmsService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

function getRealmPath (realm) {
    if (realm.parentPath === "/") {
        return realm.parentPath + realm.name;
    } else if (realm.parentPath) {
        return `${realm.parentPath}/${realm.name}`;
    } else {
        return "/";
    }
}

/**
 * This function encodes a path. It uses base64url encoding.
 * @param {string} path the path
 * @returns {string} the encoded path
 */
function encodePath (path) {
    return btoa(path)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
}

RealmsService.realms = {
    /**
     * Gets all realms.
     * @returns {Promise.<Object>} Service promise
     */
    all () {
        return RealmsService.serviceCall({
            url: fetchUrl("/global-config/realms?_queryFilter=true", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => {
            store.dispatch(setRealms(response.result));
            response.result = _(response.result).map((realm) => ({
                ...realm,
                path: getRealmPath(realm)
            })).sortBy("path").value();
            return response;
        });
    },

    /**
     * Creates a realm.
     * @param  {Object} data Complete representation of realm
     * @returns {Promise} Service promise
     */
    create (data) {
        return RealmsService.serviceCall({
            url: fetchUrl(
                "/global-config/realms?_action=create",
                { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            suppressEvents: true,
            data: JSON.stringify(data)
        });
    },

    /**
     * Gets a realm's schema together with it's values.
     * @param  {String} path Encoded realm path
     * @returns {Promise.<Object>} Service promise
     */
    get (path) {
        const collectionUrl = fetchUrl("/global-config/realms", { realm: false });

        return Promise.all([
            RealmsService.serviceCall({
                url: `${collectionUrl}?_action=schema`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }),
            RealmsService.serviceCall({
                url: `${collectionUrl}/${encodePath(path)}`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            })
        ]).then((results) => {
            const realmValues = results[1];
            store.dispatch(addRealm({ ...realmValues }));
            return {
                schema: SMSServiceUtils.sanitizeSchema(results[0]),
                values: realmValues
            };
        });
    },

    /**
     * Gets a blank realm's schema together with it's values.
     * @returns {Promise.<Object>} Service promise
     */
    schema () {
        return SMSServiceUtils.schemaWithDefaults(RealmsService, fetchUrl("/global-config/realms", {
            realm: false
        }));
    },

    /**
     * Removes a realm.
     * @param  {String} path Encoded realm path
     * @returns {Promise} Service promise
     */
    remove (path) {
        return RealmsService.serviceCall({
            url: fetchUrl(`/global-config/realms/${encodePath(path)}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "DELETE",
            suppressEvents: true
        }).then((response) => {
            store.dispatch(removeRealm(response));
        });
    },

    /**
     * Updates a realm.
     * @param  {Object} data Complete representation of realm
     * @returns {Promise} Service promise
     */
    update (data) {
        return RealmsService.serviceCall({
            url: fetchUrl(
                `/global-config/realms/${encodePath(getRealmPath(data))}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(data),
            suppressEvents: true
        }).then((response) => {
            store.dispatch(addRealm(response));
            return response;
        });
    }
};

export default RealmsService;
