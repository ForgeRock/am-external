/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/util/uri/query
 */
import _ from "lodash";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
 * @description Creates an object of key value pairs from the passed in query string
 * @param {String} paramString A string containing a query string
 * @returns {Object} An Object of key value pairs
 */
export function parseParameters (paramString) {
    const object = _.isEmpty(paramString) ? {} : _.object(_.map(paramString.split("&"), (pair) => {
        const key = pair.substring(0, pair.indexOf("="));
        const value = pair.substring(pair.indexOf("=") + 1);
        return [key, value];
    }));
    return object;
}

/**
 * @description Creates an object of key value pairs from the current url query
 * @returns {Object} An Object of key value pairs from the current url query
 */
export function getCurrentQueryParameters () {
    return this.parseParameters(URIUtils.getCurrentQueryString());
}

/**
 * @description Creates query string from an object of key value pairs
 * @param {Object} paramsObject An object of key value pairs
 * @returns {String} A query string.
 */
export function urlParamsFromObject (paramsObject) {
    if (_.isEmpty(paramsObject)) {
        return "";
    }
    return _.map(paramsObject, (value, key) => `${key}=${value}`).join("&");
}
