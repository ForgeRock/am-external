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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
 * @exports org/forgerock/openam/ui/common/util/RealmHelper
 */
const RealmHelper = {};

/**
 * Decorates a URI with an override realm
 * <p>
 * Appends a realm override to the query string if an override exists
 * @param {string} uri A URI to decorate
 * @returns {string} Decorated URI
 */
RealmHelper.decorateURLWithOverrideRealm = function (uri) {
    const overrideRealm = RealmHelper.getOverrideRealm();
    let prepend; let idx; let fragment;

    if (overrideRealm) {
        if (uri.indexOf("realm=") !== -1) {
            // URI is already decorated by some other means
            return uri;
        }
        idx = uri.indexOf("#");
        if (idx !== -1) {
            fragment = uri.slice(idx);
            uri = uri.slice(0, idx);
        }

        prepend = uri.indexOf("?") === -1 ? "?" : "&";
        uri = `${uri}${prepend}realm=${overrideRealm}`;
        if (fragment) {
            uri = uri + fragment;
        }
    }

    return uri;
};

/**
 * Decorates a URI with realm information
 * <p>
 * Delegates to #decorateURIWithSubRealm & #decorateURLWithOverrideRealm
 * @param {string} uri A URI to decorate
 * @returns {string} Decorated URI
 */
RealmHelper.decorateURIWithRealm = function (uri) {
    uri = RealmHelper.decorateURIWithSubRealm(uri);
    uri = RealmHelper.decorateURLWithOverrideRealm(uri);

    return uri;
};

/**
 * Decorates a URI with a sub realm
 * <p>
 * Replaces any occurance of '__subrealm__/' in the URI with the sub realm
 * @param {string} uri A URI to decorate
 * @returns {string} Decorated URI
 */
RealmHelper.decorateURIWithSubRealm = function (uri) {
    const persisted = Configuration.globalData;
    const persistedSubRealm = persisted && persisted.auth ? persisted.auth.subRealm : "";
    const subRealm = persistedSubRealm ? `${persistedSubRealm}/` : "";

    if (persisted &&
        persisted.auth &&
        (persisted.auth.subRealm === undefined || persisted.auth.subRealm === null)) {
        console.warn("Unable to decorate URI, Configuration.globalData.auth.subRealm not yet set");
    }

    uri = uri.replace("__subrealm__/", subRealm);

    return uri;
};

/**
 * Determines the current realm by examining both the override realm and the subRealm.
 * The subRealm is determined from the /XUI/#login/realmName format.
 * The overrideRealm is determined from either the /XUI/?realm=/realmName#login/ or /XUI/#login/&realm=/realmName
 * format.
 *
 * Please note that the realm value determined by XUI does not take DNS aliases into account, hence it can be
 * potentially incorrect when overrideRealm is not specified in the request.
 * @returns {string} The realm determined from the request without the leading '/'.
 */
RealmHelper.getRealm = function () {
    const realm = RealmHelper.getOverrideRealm() || RealmHelper.getSubRealm();
    return realm.substring(0, 1) === "/" ? realm.substring(1) : realm;
};

/**
 * Determines the current override realm from the URI query string and hash fragment query string
 * @returns {string} Override realm AS IS (no slash modification) (e.g. <code>/</code> or <code>/realm1</code>)
 */
RealmHelper.getOverrideRealm = function () {
    // Note: unlike in other places, the URI query parameter takes precedence over the fragment query parameter
    const uri = URIUtils.parseQueryString(URIUtils.getCurrentQueryString()).realm;
    const fragment = URIUtils.parseQueryString(URIUtils.getCurrentFragmentQueryString()).realm;

    return uri ? uri : fragment;
};

/**
 * Determines the current sub realm from the URI hash fragment
 * @returns {string} Sub realm WITHOUT any leading or trailing slash (e.g. <code>realm1/realm2</code>)
 */
RealmHelper.getSubRealm = function () {
    const subRealmSplit = URIUtils.getCurrentFragment().split("/");
    const page = subRealmSplit.shift().split("&")[0];
    const subRealmSpecifiablePages =
            ["login", "passwordReset", "continuePasswordReset", "register", "continueRegister"];
    let subRealm;

    if (page && _.includes(subRealmSpecifiablePages, page)) {
        subRealm = subRealmSplit.join("/").split("&")[0];
        subRealm = subRealm.slice(-1) === "/" ? subRealm.slice(0, -1) : subRealm;
    } else if (Configuration.globalData.auth.subRealm) {
        subRealm = Configuration.globalData.auth.subRealm;
    } else {
        subRealm = "";
    }

    return subRealm;
};

/**
 * Encode a realm's path.
 * @param  {string} path - realm path
 * @returns {string} Encoded realm path e.g. /myRealm/Realm%232
 */
RealmHelper.encodeRealm = function (path) {
    const encodedPath = [];
    const realmPath = path.split("/");

    _.each(realmPath, (pathFragment) => {
        if (pathFragment !== "") {
            encodedPath.push(encodeURIComponent(pathFragment));
        }
    });

    return `/${encodedPath.join("/")}`;
};

export default RealmHelper;
