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
 * Copyright 2015-2018 ForgeRock AS.
 */

import Constants from "org/forgerock/openam/ui/common/util/Constants";

import KBADelegate from "org/forgerock/commons/ui/user/delegates/KBADelegate";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import store from "store";
// Update the service URL on the KBADelegate to use the realm, once the session has been loaded.
let realm;

function checkRealmInState () {
    const storedRealm = store.getState().local.session.realm;
    if (storedRealm !== realm) {
        realm = storedRealm;
        const path = fetchUrl(`/${Constants.SELF_SERVICE_CONTEXT}`);
        KBADelegate.serviceUrl = `${Constants.context}/json${path}`;
        KBADelegate.baseEntity = `json${path}`;
    }
}

checkRealmInState();

store.subscribe(checkRealmInState);

export default KBADelegate;
