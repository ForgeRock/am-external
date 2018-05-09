/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/util/isRealmChanged
 */
import store from "store/index";

export default function isRealmChanged () {
    const sessionInfoIntendedRealm = store.getState().remote.info.realm;
    const authenticatedRealm = store.getState().local.session.realm;

    return sessionInfoIntendedRealm !== authenticatedRealm;
}
