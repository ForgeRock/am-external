/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
* @module org/forgerock/openam/ui/admin/services/realm/PoliciesService
*/
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/utils/AdministeredRealmsHelper",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], (_, AbstractDelegate, Constants, AdministeredRealmsHelper, fetchUrl, RealmHelper) => {
    var obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`),
        getCurrentAdministeredRealm = function () {
            var realm = AdministeredRealmsHelper.getCurrentRealm();
            return realm === "/" ? "" : RealmHelper.encodeRealm(realm);
        };

    obj.getApplicationType = function (type) {
        return obj.serviceCall({
            url: fetchUrl.default(`/applicationtypes/${type}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getDecisionCombiners = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/decisioncombiners/?_queryId=&_fields=title", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getEnvironmentConditions = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/conditiontypes?_queryId=&_fields=title,logical,config", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getSubjectConditions = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/subjecttypes?_queryId=&_fields=title,logical,config", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getAllUserAttributes = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/subjectattributes?_queryFilter=true", { realm: getCurrentAdministeredRealm() }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.queryIdentities = function (name, query) {
        return obj.serviceCall({
            url: fetchUrl.default(`/${name}?_queryId=${query}*`, { realm: getCurrentAdministeredRealm() }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getUniversalId = function (name, type) {
        return obj.serviceCall({
            url: fetchUrl.default(`/${type}/${name}?_fields=universalid`, { realm: getCurrentAdministeredRealm() }),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0" }
        });
    };

    obj.getDataByType = function (type) {
        return obj.serviceCall({
            url: fetchUrl.default(`/${type}?_queryFilter=true`, { realm: getCurrentAdministeredRealm() }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getScriptById = function (id) {
        return obj.serviceCall({
            url: fetchUrl.default(`/scripts/${id}`, { realm: getCurrentAdministeredRealm() }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getAllRealms = function () {
        return obj.serviceCall({
            url: fetchUrl.default("/realms?_queryFilter=true", { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.importPolicies = function (data) {
        return obj.serviceCall({
            serviceUrl: `${Constants.host}${Constants.context}`,
            url: fetchUrl.default(`/xacml${getCurrentAdministeredRealm()}/policies`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data
        });
    };

    obj.listResourceTypes = function () {
        return obj.serviceCall({
            url: fetchUrl.default(
                `/resourcetypes?_queryFilter=name+eq+${encodeURIComponent('"^(?!Delegation Service$).*"')}`,
                { realm: getCurrentAdministeredRealm() }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    return obj;
});
