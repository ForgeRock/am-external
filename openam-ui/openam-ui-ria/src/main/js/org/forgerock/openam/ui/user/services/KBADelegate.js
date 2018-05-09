/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/delegates/KBADelegate",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "store/index"
], (Constants, KBADelegate, fetchUrl, store) => {
    // Update the service URL on the KBADelegate to use the realm, once the session has been loaded.
    let realm;

    function checkRealmInState () {
        const storedRealm = store.default.getState().local.session.realm;
        if (storedRealm !== realm) {
            realm = storedRealm;
            const path = fetchUrl.default(`/${Constants.SELF_SERVICE_CONTEXT}`);
            KBADelegate.serviceUrl = `${Constants.context}/json${path}`;
            KBADelegate.baseEntity = `json${path}`;
        }
    }

    checkRealmInState();

    store.default.subscribe(checkRealmInState);

    return KBADelegate;
});