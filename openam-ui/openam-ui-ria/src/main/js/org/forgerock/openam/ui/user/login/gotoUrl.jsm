/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
