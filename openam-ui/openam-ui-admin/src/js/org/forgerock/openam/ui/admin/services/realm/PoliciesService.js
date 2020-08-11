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

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import AdministeredRealmsHelper from "org/forgerock/openam/ui/admin/utils/AdministeredRealmsHelper";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";

/**
* @module org/forgerock/openam/ui/admin/services/realm/PoliciesService
*/

const PoliciesService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);
const getCurrentAdministeredRealm = function () {
    const realm = AdministeredRealmsHelper.getCurrentRealm();
    return realm === "/" ? "" : RealmHelper.encodeRealm(realm);
};

PoliciesService.getApplicationType = function (type) {
    return PoliciesService.serviceCall({
        url: fetchUrl(`/applicationtypes/${type}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getDecisionCombiners = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl("/decisioncombiners/?_queryId=&_fields=title", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getEnvironmentConditions = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl("/conditiontypes?_queryId=&_fields=title,logical,config", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getSubjectConditions = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl("/subjecttypes?_queryId=&_fields=title,logical,config", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getAllUserAttributes = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl("/subjectattributes?_queryFilter=true", { realm: getCurrentAdministeredRealm() }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.queryIdentities = function (name, query) {
    return PoliciesService.serviceCall({
        url: fetchUrl(`/${name}?_queryId=${query}*`, { realm: getCurrentAdministeredRealm() }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getUniversalId = function (name, type) {
    return PoliciesService.serviceCall({
        url: fetchUrl(`/${type}/${name}?_fields=universalid`, { realm: getCurrentAdministeredRealm() }),
        headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0" }
    });
};

PoliciesService.getDataByType = function (type) {
    return PoliciesService.serviceCall({
        url: fetchUrl(`/${type}?_queryFilter=true`, { realm: getCurrentAdministeredRealm() }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getScriptById = function (id) {
    return PoliciesService.serviceCall({
        url: fetchUrl(`/scripts/${id}`, { realm: getCurrentAdministeredRealm() }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.getAllRealms = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl("/realms?_queryFilter=true", { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

PoliciesService.importPolicies = function (data) {
    return PoliciesService.serviceCall({
        serviceUrl: `${Constants.host}${Constants.context}`,
        url: fetchUrl(`/xacml${getCurrentAdministeredRealm()}/policies`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        data
    });
};

PoliciesService.listResourceTypes = function () {
    return PoliciesService.serviceCall({
        url: fetchUrl(
            `/resourcetypes?_queryFilter=name+eq+${encodeURIComponent('"^(?!Delegation Service$).*"')}`,
            { realm: getCurrentAdministeredRealm() }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    });
};

export default PoliciesService;
