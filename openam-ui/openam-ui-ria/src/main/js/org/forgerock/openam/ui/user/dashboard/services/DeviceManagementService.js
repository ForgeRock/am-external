/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/user/dashboard/services/DeviceManagementService
 */
define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], ($, AbstractDelegate, Configuration, Constants, fetchUrl) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    /**
     * Delete oath device by uuid
     * @param {String} uuid The unique device id
     * @returns {Promise} promise that will contain the response
     */
    obj.remove = function (uuid) {
        const loggedUserUid = Configuration.loggedUser.get("username");
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${loggedUserUid}/devices/2fa/oath/${uuid}`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            suppressEvents: true,
            method: "DELETE"
        });
    };

    /**
     * Set status of the oath skip flag for devices
     * @param {Boolean} skip The flag value
     * @returns {Promise} promise that will contain the response
     */
    obj.setDevicesOathSkippable = function (skip) {
        const loggedUserUid = Configuration.loggedUser.get("username");
        const skipOption = { value: skip };
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${loggedUserUid}/devices/2fa/oath?_action=skip`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            data: JSON.stringify(skipOption),
            suppressEvents: true,
            method: "POST"
        });
    };

    /**
     * Check status of the oath skip flag for devices
     * @returns {Promise} promise that will contain the response
     */
    obj.checkDevicesOathSkippable = function () {
        const loggedUserUid = Configuration.loggedUser.get("username");
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${loggedUserUid}/devices/2fa/oath?_action=check`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            suppressEvents: true,
            method: "POST"
        }).then((statusData) => {
            return statusData.result;
        });
    };

    /**
     * Get array of oath devices
     * @returns {Promise} promise that will contain the response
     */
    obj.getAll = function () {
        const loggedUserUid = Configuration.loggedUser.get("username");
        return obj.serviceCall({
            url: fetchUrl.default(`/users/${loggedUserUid}/devices/2fa/oath?_queryFilter=true`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            suppressEvents: true
        }).then((value) => value.result);
    };

    return obj;
});
