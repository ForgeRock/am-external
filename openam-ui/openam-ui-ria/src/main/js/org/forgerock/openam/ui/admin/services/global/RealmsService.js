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
 * Copyright 2016-2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/RealmsService
 */
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Promise",
    "store/index",
    "store/modules/remote/realms"
], (_, AbstractDelegate, Constants, SMSServiceUtils, fetchUrl, Promise, store, realmsActionCreators) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

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

    obj.realms = {
        /**
         * Gets all realms.
         * @returns {Promise.<Object>} Service promise
         */
        all () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/realms?_queryFilter=true", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).done((response) => {
                store.default.dispatch(realmsActionCreators.setRealms(response.result));
                response.result = _(response.result).map((realm) => ({
                    ...realm,
                    path: getRealmPath(realm)
                })).sortBy("path").value();
            });
        },

        /**
         * Creates a realm.
         * @param  {Object} data Complete representation of realm
         * @returns {Promise} Service promise
         */
        create (data) {
            return obj.serviceCall({
                url: fetchUrl.default(
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
            const collectionUrl = fetchUrl.default("/global-config/realms", { realm: false });

            return Promise.all([
                obj.serviceCall({
                    url: `${collectionUrl}?_action=schema`,
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }),
                obj.serviceCall({
                    url: `${collectionUrl}/${encodePath(path)}`,
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                })
            ]).then((results) => {
                const realmValues = results[1][0];
                store.default.dispatch(realmsActionCreators.addRealm({ ...realmValues }));
                return {
                    schema: SMSServiceUtils.sanitizeSchema(results[0][0]),
                    values: realmValues
                };
            });
        },

        /**
         * Gets a blank realm's schema together with it's values.
         * @returns {Promise.<Object>} Service promise
         */
        schema () {
            return SMSServiceUtils.schemaWithDefaults(obj, fetchUrl.default("/global-config/realms", {
                realm: false
            }));
        },

        /**
         * Removes a realm.
         * @param  {String} path Encoded realm path
         * @returns {Promise} Service promise
         */
        remove (path) {
            return obj.serviceCall({
                url: fetchUrl.default(`/global-config/realms/${encodePath(path)}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE",
                suppressEvents: true
            }).then((response) => {
                store.default.dispatch(realmsActionCreators.removeRealm(response));
            });
        },

        /**
         * Updates a realm.
         * @param  {Object} data Complete representation of realm
         * @returns {Promise} Service promise
         */
        update (data) {
            return obj.serviceCall({
                url: fetchUrl.default(
                    `/global-config/realms/${encodePath(getRealmPath(data))}`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data),
                suppressEvents: true
            }).then((response) => {
                store.default.dispatch(realmsActionCreators.addRealm(response));
                return response;
            });
        }
    };

    return obj;
});
