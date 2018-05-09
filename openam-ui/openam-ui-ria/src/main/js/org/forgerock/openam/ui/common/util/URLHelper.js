/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/admin/utils/AdministeredRealmsHelper",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], (Configuration, AdministeredRealmsHelper, Constants, RealmHelper) => {
    return {
        substitute (url) {
            return function () {
                var realm = AdministeredRealmsHelper.getCurrentRealm(),
                    apiUrlBase = `${Constants.host}${Constants.context}/json${
                        (realm === "/" ? "" : RealmHelper.encodeRealm(realm))
                    }`;

                return url.replace("__api__", apiUrlBase)
                    .replace("__host__", Constants.host)
                    .replace("__context__", Constants.context)
                    .replace("__username__", Configuration.loggedUser.get("username"));
            };
        }
    };
});
