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
 * Copyright 2020 ForgeRock AS.
 */

import Constants from "org/forgerock/openam/ui/common/util/Constants";

/**
 * Replace the placeholders with the replacement values
 */
const replacePlaceHolders = (content, placeHolderValueMap) => {
    let jsonString = JSON.stringify(content);
    for (var placeHolder in placeHolderValueMap) {
        if (placeHolderValueMap[placeHolder]) {
            jsonString = jsonString.replace(new RegExp(placeHolder, "g"), placeHolderValueMap[placeHolder]);
        }
    }
    return JSON.parse(jsonString);
};

const sanitiseBaseUrl = (baseUrl) => {
    return baseUrl.replace(/\/$/, "");
};

const sanitiseMetaAlias = (metaAlias) => {
    if (metaAlias && metaAlias.charAt(0) !== "/") {
        metaAlias = "/".concat(metaAlias);
    }
    return metaAlias;
};

/**
* Replace the base url and idp meta alias place holders in the schema content.
* @param {object} content The schema content.
* @param {string} baseUrl The base url to be replaced with on the schema content.
* @param {string} idpMetaAlias The idp meta alias to be replaced with on the schema content.
**/
export const replaceIdpPlaceHolders = (content, baseUrl, idpMetaAlias) => {
    const placeHolderValueMap = {
        "{baseUrl}" : sanitiseBaseUrl(baseUrl),
        "{idpMetaAlias}" : sanitiseMetaAlias(idpMetaAlias)
    };
    return replacePlaceHolders(content, placeHolderValueMap);
};

/**
* Replace the base url and sp meta alias place holders in the schema content.
* @param {object} content The schema content.
* @param {string} baseUrl The base url to be replaced with on the schema content.
* @param {string} spMetaAlias The sp meta alias to be replaced with on the schema content.
**/
export const replaceSpPlaceHolders = (content, baseUrl, spMetaAlias) => {
    const placeHolderValueMap = {
        "{baseUrl}" : sanitiseBaseUrl(baseUrl),
        "{spMetaAlias}" : sanitiseMetaAlias(spMetaAlias)
    };
    return replacePlaceHolders(content, placeHolderValueMap);
};

export const getDefaultUrl = () => {
    const context = Constants.context;
    const defaultBaseUrl = window.location.protocol.concat("//").concat(window.location.host).concat(context);
    return defaultBaseUrl;
};
