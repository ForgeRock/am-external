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
import jqueryI18next from "jquery-i18next";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import locales from "i18next-resource-store-loader!locales/index.js";
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
 * @param {object} [options] Options.
 * @param {string} [options.language] Language to use (overrides language detection).
 * @param {string} [options.namespace=translation] Namespace to load.
 * @returns {Promise} A Promise, resolved when Initialisation is complete.
 */
export function init ({ language, namespace = "translation" } = {}) {
    return new Promise((resolve) => {
        registerHelpers();

        const detector = new I18nextBrowserLanguagedetector();
        detector.addDetector(serverDetector);

        i18next
            .use(detector)
            .init({
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
                nsSeparator: ":::",
                resources: locales
            }, resolve);

        jqueryI18next.init(i18next, $);
    });
}

export function getLanguage () {
    return i18next.languages ? i18next.languages[0] : undefined;
}