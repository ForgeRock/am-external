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
 * Copyright 2014-2025 Ping Identity Corporation.
 */

import { get } from "lodash";
import { t } from "i18next";
import Handlebars from "handlebars-template-loader/runtime";

const register = () => {
    /**
     * Handlebars parameterized translation helper
     * @param {Object|String} [options] Object or String to pass to this function.
     * @param {Boolean} [options.hash.safeString] If set to false the returned string will be html character encoded
     * @returns {Object|String} returns a translation object or string if safeString was set to false.
     * @example
     * 1) In translation file define a value under "key.to.my.translation.string" key,
     *    e.g. "Display __foo__ and __bar__"
     * 2) call helper function with key value pairs: {{t "key.to.my.translation.string" foo="test1" bar="test2"}}
     * 3) Resulting string will be "Display test1 and test2"
     */
    Handlebars.registerHelper("t", (key, options) => {
        options = options || {};
        if (get(options.hash, "safeString") !== false) {
            return new Handlebars.SafeString(t(key, options.hash));
        } else {
            // TODO: OPENAM-9618 The safeString check must remain until we use
            // triple handlebars syntax for safe strings.
            return t(key, options.hash);
        }
    });
};

export default register;
