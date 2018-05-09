/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/global/ScriptsService
 */
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (_, AbstractDelegate, Constants, fetchUrl) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    obj.scripts = {

        /**
         * Gets the list of default scripts.
         * @param {String} subSchemaType SubSchema type
         * @returns {Promise.<Object>} promise with the list of default scripts
         */
        getAllDefault (subSchemaType) {
            return obj.serviceCall({
                url: fetchUrl.default("/scripts?_pageSize=10&_sortKeys=name&_queryFilter=default eq true and context " +
                    `eq "${subSchemaType}"&_pagedResultsOffset=0`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then((response) => _.sortBy(response.result, "name"));
        },

        /**
         * Gets all script's contexts.
         * @returns {Promise.<Object>} Service promise
         */
        getAllContexts () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/services/scripting/contexts?_queryFilter=true", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },

        /**
         * Gets a default global script's context.
         * @returns {Promise.<Object>} Service promise
         */
        getDefaultGlobalContext () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/services/scripting", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },

        /**
         * Gets a script's schema.
         * @returns {Promise.<Object>} Service promise
         */
        getSchema () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/services/scripting?_action=schema", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });
        },

        /**
         * Gets a script context's schema.
         * @returns {Promise.<Object>} Service promise
         */
        getContextSchema () {
            return obj.serviceCall({
                url: fetchUrl.default("/global-config/services/scripting/contexts?_action=schema", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });
        }
    };

    return obj;
});
