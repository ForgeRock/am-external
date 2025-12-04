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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

import { isFunction } from "lodash";
import debug from "debug";

import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";

/**
 * Loads a template.
 * @param {string|Function} pathOrModule Template path or a template module.
 * @param {string} [themePath] Theme path. Must contain a trailing slash.
 * @returns {Promise<Function>} Promise, resolved with template module.
 */
const loadTemplate = async (pathOrModule, themePath) => {
    const logger = debug("forgerock:am:user:view:template");

    if (isFunction(pathOrModule)) {
        return pathOrModule;
    } else {
        let module;

        try {
            module = await import(`themes/${themePath}templates/${pathOrModule}.html`);
            logger(`Template \`${pathOrModule}\` from \`${themePath}\` theme loaded.`);
        } catch (error) {
            module = await import(`themes/default/templates/${pathOrModule}.html`);
            logger(`Template \`${pathOrModule}\` from \`default\` theme loaded.`);
        }

        return unwrapDefaultExport(module);
    }
};

export default loadTemplate;
