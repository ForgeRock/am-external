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
 * Copyright 2015-2017 ForgeRock AS.
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
