/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString
 */
import _ from "lodash";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

/**
 * @returns {String} The current fragment query string or an empty string.
 */
const getCurrentFragmentParamString = () => {
    const params = URIUtils.getCurrentFragmentQueryString();
    return _.isEmpty(params) ? "" : `&${params}`;
};

export default getCurrentFragmentParamString;
