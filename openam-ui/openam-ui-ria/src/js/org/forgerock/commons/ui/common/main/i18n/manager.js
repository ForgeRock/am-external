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

import $ from "jquery";
import i18next from "i18next";
import I18nextBrowserLanguagedetector from "i18next-browser-languagedetector";
import i18nextXHRBackend from "i18next-xhr-backend";
import jqueryI18next from "jquery-i18next";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import registerHelpers from "./helpers/register";
import serverDetector from "./detectors/server";

/**
 * Initialises I18n.
 * 
 * Language detection takes place in the following order:
 * 1) Query string. e.g. ?locale=fr
 * 2) Server language. e.g. Server info => { lang: "en-US" }
 * 3) Cookie.
 * 4) Default as defined in Constants.DEFAULT_LANGUAGE.
 * @param {Object} [options] Options.
 * @param {String} [options.language] Language to use (overrides language detection).
 * @param {String} [options.loadPath] Path where resources get loaded from, or a function.
 * @param {String} [options.namespace=translation] Namespace to load.
 * @returns {Deferred} A deferred, resolved when Initialisation is complete.
 */
export function init ({
    language,
    loadPath = "./locales/__lng__/__ns__.json",
    namespace = "translation"
} = {}) {
    const deferred = $.Deferred();

    registerHelpers();

    const detector = new I18nextBrowserLanguagedetector();
    detector.addDetector(serverDetector);

    i18next
        .use(i18nextXHRBackend)
        .use(detector)
        .init({
            backend: {
                loadPath
            },
            defaultNS: namespace,
            detection: {
                lookupCookie: "i18next",
                lookupQuerystring: "locale",
                order: ["querystring", "cookie", "server"]
            },
            fallbackLng: Constants.DEFAULT_LANGUAGE,
            interpolation: {
                prefix: "__",
                suffix: "__"
            },
            lng: language,
            load: "all",
            ns: namespace,
            nsSeparator: ":::"
        }, deferred.resolve);

    jqueryI18next.init(i18next, $);

    return deferred;
}

export function getLanguage () {
    return i18next.languages ? i18next.languages[0] : undefined;
}
