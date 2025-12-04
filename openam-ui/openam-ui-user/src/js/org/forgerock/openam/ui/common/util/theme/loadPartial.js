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
import Handlebars from "handlebars-template-loader/runtime";

import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";

/**
 * Loads and registers a partial with Handlebars.
 * @param {string} name Partial name.
 * @param {string|Function} pathOrModule Partial path or a partial module.
 * @param {string} [themePath] Theme path. Must contain a trailing slash.
 * @returns {Promise<Function>} Promise, resolved with partial module.
 */
const loadPartial = async (name, pathOrModule, themePath) => {
    const logger = debug("forgerock:am:user:view:partial");
    const register = (name, partial) => Handlebars.registerPartial(name, partial);

    if (isFunction(pathOrModule)) {
        register(name, pathOrModule);
        return pathOrModule;
    } else {
        let module;

        try {
            module = await import(`themes/${themePath}partials/${pathOrModule}.html`);
            logger(`Partial \`${pathOrModule}\` from \`${themePath}\` theme loaded.`);
        } catch (error) {
            module = await import(/* webpackMode: "eager" */`themes/default/partials/${pathOrModule}.html`);
            logger(`Partial \`${pathOrModule}\` from \`default\` theme loaded.`);
        }

        register(name, unwrapDefaultExport(module));
        return module;
    }
};

export default loadPartial;
