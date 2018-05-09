/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/services/fetchUrl
 */
import store from "store/index";

const ROOT_REALM_IDENTIFIER = "/root"; // TODO "root" is a placeholder. Identifier for a root realm yet undecided.
const hasLeadingSlash = (value) => value[0] === "/";
const throwIfPathHasNoLeadingSlash = (path) => {
    if (!hasLeadingSlash(path)) {
        throw new Error(`[fetchUrl] Path must start with forward slash. "${path}"`);
    }
};
const normaliseRealmAliasResourcePath = (alias) => `/realms/${alias}`;
const normaliseRealmResourcePath = (realm) => realm.replace(/\//g, "/realms/");
const redesignateRootRealm = (realm, rootIdentifier) => {
    const isRootRealm = realm === "/";
    return isRootRealm ? rootIdentifier : `${rootIdentifier}${realm}`;
};

/**
 * Fetch a URL using the newer method of laying realm information into the URL (e.g. Redux).
 *
 * @param {string} path Path to the resource. Must start with a forward slash.
 * @param {Object} [options] Options to pass to this function.
 * @param {string} [options.realm=store.getState().local.session.realm] The realm to use when constructing the URL. Maybe an absolute realm or alias.
 * @returns {string} URL string to be appended after the <code>.../json</code> path.
 *
 * @throws {Error} If path does not start with a forward slash.
 *
 * @example // With session on the root realm
 * fetchUrl("/authentication") => "/realms/root/authentication"
 * @example // With session on a sub realm
 * fetchUrl("/authentication") => "/realms/root/realms/myRealm/authentication"
 * @example // Forcing a realm
 * fetchUrl("/authentication", { realm: "/myRealm" }) => "/realms/root/realms/myRealm/authentication"
 * @example // Forcing no realm
 * fetchUrl("/authentication", { realm: false }) => "/authentication"
 * @example // With realm alias
 * fetchUrl("/authentication", { realm: "myAlias" }) => "/realms/myAlias/authentication"
 */
const fetchUrl = (path, { realm = store.getState().local.session.realm } = {}) => {
    throwIfPathHasNoLeadingSlash(path);
    if (!realm) { return path; }

    if (hasLeadingSlash(realm)) {
        realm = redesignateRootRealm(realm, ROOT_REALM_IDENTIFIER);
        realm = normaliseRealmResourcePath(realm);
    } else {
        realm = normaliseRealmAliasResourcePath(realm);
    }

    return realm + path;
};

export default fetchUrl;
