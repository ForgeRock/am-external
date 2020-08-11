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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/applications/agents/common/agentRootUrl
 */

import { get, forEach } from "lodash";

const AGENT_ROOT_URL = "agentRootURL=";

/**
 * Removes the prefix "agentRootURL=" from all the values in the array "urls"
 * @param {string} values The json values containing the urls to edit
 * @param {string} urlsPath The path to the urls
 * @returns {string} Returns all the input values, now containing urls without the prefix
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
 * @param {string} values The json values containing the urls to edit
 * @param {string} urlsPath The path to the urls
 * @returns {string} Returns all the input values, now containing urls with the prefix
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
