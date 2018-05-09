/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import Handlebars from "handlebars-template-loader/runtime";
import i18next, { t } from "i18next";

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
        if (_.get(options.hash, "safeString") !== false) {
            return new Handlebars.SafeString(t(key, options.hash));
        } else {
            // TODO: OPENAM-9618 The safeString check must remain until we use
            // triple handlebars syntax for safe strings.
            return t(key, options.hash);
        }
    });

    /**
     * @param {object} map Each key in the map is a locale, each value is a string in that locale
     * @example
        {{mapTranslate map}} where map is an object like so:
        {
            "en_GB": "What's your favorite colour?",
            "fr": "Quelle est votre couleur préférée?",
            "en": "What's your favorite color?"
        }
    */
    Handlebars.registerHelper("mapTranslate", (map) => {
        let fallback;
        if (_.has(map, i18next.options.lng)) {
            return new Handlebars.SafeString(map[i18next.options.lng]);
        } else {
            fallback = _.find(i18next.options.fallbackLng, (lng) => {
                return _.has(map, lng);
            });
            return new Handlebars.SafeString(map[fallback]);
        }
    });
};

export default register;
