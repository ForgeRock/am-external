/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/applications/agents/common/agentRootUrl
 */

import { get, forEach } from "lodash";

const AGENT_ROOT_URL = "agentRootURL=";

/**
 * Removes the prefix "agentRootURL=" from all the values in the array "urls"
 * @param {String} values The json values containing the urls to edit
 * @param {String} urlsPath The path to the urls
 * @returns {String} Returns all the input values, now containing urls without the prefix
 */
export const removeAgentRootURLPrefix = (values, urlsPath) => {
    const urls = get(values, urlsPath);

    forEach(urls.value, (url, index) => {
        if (url && url.indexOf(AGENT_ROOT_URL) === 0) {
            urls.value[index] = url.substring(AGENT_ROOT_URL.length);
        }
    });

    return values;
};

/**
 * Adds the prefix "agentRootURL=" to all the values in the array "urls", if not already present
 * @param {String} values The json values containing the urls to edit
 * @param {String} urlsPath The path to the urls
 * @returns {String} Returns all the input values, now containing urls with the prefix
 */
export const addAgentRootURLPrefix = (values, urlsPath) => {
    const urls = get(values, urlsPath);

    forEach(urls.value, (url, index) => {
        if (url && url.indexOf(AGENT_ROOT_URL) !== 0) {
            urls.value[index] = AGENT_ROOT_URL.concat(url);
        }
    });

    return values;
};
