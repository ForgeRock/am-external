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
 * Copyright 2016-2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/user/login/gotoUrl
 */

import _ from "lodash";
import $ from "jquery";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import validateGotoService from "org/forgerock/openam/ui/user/services/validateGotoService";

export function ifRelativePathInsertContext (path) {
    let context = "";
    if (path.indexOf("/") === 0 && path.indexOf(`${Constants.context}`) !== 0) {
        context = `${Constants.context}`;
    }
    return context + path;
}

export function exists () {
    return _.has(Configuration, "globalData.auth.validatedGoto");
}

export function get () {
    return _.get(Configuration, "globalData.auth.validatedGoto");
}

export function isNotDefaultPath (url) {
    // The server will return the CONSOLE_PATH if the there is no (valid) goto to return.
    return url !== Constants.CONSOLE_PATH;
}

export function remove () {
    if (this.exists()) {
        delete Configuration.globalData.auth.validatedGoto;
    }
}

export function setValidated (value) {
    // Any goto url coming from the url params needs to be validated. And success or goto url coming from the server
    // will have already been validated and will be returned unencoded
    _.set(Configuration, "globalData.auth.validatedGoto", encodeURIComponent(this.ifRelativePathInsertContext(value)));
}

export function toHref () {
    return decodeURIComponent(Configuration.globalData.auth.validatedGoto);
}

/**
 * Returns a promise that containes the successURL or undefined.
 * Instead of returning the CONSOLE_PATH which the server returns if the supplied url was invalid,
 * it returns an empty promise.
 * @param {String} unvalidated The url or path to validate.
 * @returns {Promise} A promise containing the successURL or undefined.
 */
export function validateParam (unvalidated) {
    const deferred = $.Deferred();
    validateGotoService(unvalidated).then((responce) => {
        if (this.isNotDefaultPath(responce.successURL)) {
            deferred.resolve(responce.successURL);
        } else if (this.isNotDefaultPath(unvalidated)) {
            deferred.reject();
        } else {
            deferred.resolve(responce.successURL);
        }
    }, () => {
        deferred.reject();
    });
    return deferred;
}
