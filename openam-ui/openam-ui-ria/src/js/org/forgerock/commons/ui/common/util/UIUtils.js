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
 * Copyright 2011-2018 ForgeRock AS.
 */

import _ from "lodash";
import Handlebars from "handlebars-template-loader/runtime";

import { getTheme } from "ThemeManager";
import Router from "org/forgerock/commons/ui/common/main/Router";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";

/**
 * @exports org/forgerock/commons/ui/common/util/UIUtils
 */
const UIUtils = {
    configuration: {
        partialPaths: {
            "form/_JSONSchemaFooter": "form/_JSONSchemaFooter",
            "form/_AutoCompleteOffFix": "form/_AutoCompleteOffFix",
            "form/_Button": "form/_Button",
            "form/_Select": "form/_Select",
            "headers/_Title": "headers/_Title",
            "headers/_TitleWithSubAndIcon": "headers/_TitleWithSubAndIcon",
            "login/_Choice": "login/_Choice",
            "login/_Confirmation": "login/_Confirmation",
            "login/_Default": "login/_Default",
            "login/_HiddenValue": "login/_HiddenValue",
            "login/_Password": "login/_Password",
            "login/_Redirect": "login/_Redirect",
            "login/_RememberLogin": "login/_RememberLogin",
            "login/_ScriptTextOutput": "login/_ScriptTextOutput",
            "login/_SelfService": "login/_SelfService",
            "login/_SocialAuthn": "login/_SocialAuthn",
            "login/_TextInput": "login/_TextInput",
            "login/_TextOutput": "login/_TextOutput",
            "login/_PollingWait": "login/_PollingWait"
        }
    }
};

UIUtils.templates = {};

/**
 * Renders the template.
 * @param {String} templateUrl - template url.
 * @param {JQuery} el - element, in which the template should be rendered.
 * @param {Object} data - template will be compiled with this data.
 * @param {Function} callback - callback to be called after template is rendered.
 * @param {String} mode - "append" means the template will be appended, provide any other value for
 *                        replacing current contents of the element.
 * @param {Function} validation - validation function.
 */
UIUtils.renderTemplate = function (templateUrl, el, data, callback, mode, validation) {
    let compileTemplate;
    if (templateUrl) {
        compileTemplate = templateUrl(data);
    }

    if (validation && !validation()) {
        return false;
    }

    if (mode === "append") {
        el.append(compileTemplate);
    } else {
        el.html(compileTemplate);
    }

    if (callback) {
        callback();
    }
};

/**
 * Loads all the Handlebars partials defined in the "partialPaths" attribute of this module's configuration
 */
UIUtils.preloadInitialPartials = function () {
    getTheme().then((theme) => {
        _.each(UIUtils.configuration.partialPaths, (partialPath, name) => {
            const registerPartial = (partial) => Handlebars.registerPartial(name, partial);
            const importDefaultPartial = (path) =>
                import(`partials/${path}.html`).then(unwrapDefaultExport);
            const importThemePartial = (themePath, partialPath) =>
                import(`themes/${themePath}partials/${partialPath}.html`).then(unwrapDefaultExport);

            if (theme.path) {
                return importThemePartial(theme.path, partialPath).then(registerPartial, () => {
                    console.log(`Loading custom partial "${partialPath}" failed. Falling back to default.`);
                    importDefaultPartial(partialPath).then(registerPartial);
                });
            } else {
                importDefaultPartial(partialPath).then(registerPartial);
            }
        });
    });
};

/**
 * @description A handlebars helper checking the equality of two provided parameters, if
 *      the parameters are not equal and there is an else block, the else block will be rendered.
 *
 * @example:
 *
 * {{#equals "testParam" "testParam"}}
 *      <span>Equals Block!</span>
 * {{else}}
 *      <span> Not Equals Block!</span>
 * {{/equals}}
 */
Handlebars.registerHelper("equals", function (val, val2, options) {
    if (val === val2) {
        return options.fn(this);
    } else {
        return options.inverse(this);
    }
});

Handlebars.registerHelper("stringify", (string, spaces) => {
    spaces = spaces ? spaces : 0;
    const newString = JSON.stringify(string, null, spaces);
    return newString;
});

Handlebars.registerHelper("ifObject", function (item, options) {
    if (typeof item === "object") {
        return options.fn(this);
    } else {
        return options.inverse(this);
    }
});

/**
 * Handlebars 'routeTo' helper
 * Creates a routing hash will all arguments passed through #encodeURIComponent
 */
Handlebars.registerHelper("routeTo", function (routeKey) {
    let result = "#",
        args = _.toArray(arguments).slice(1, -1);
    args = _.map(args, (arg) => {
        return encodeURIComponent(arg);
    });

    result += Router.getLink(Router.configuration.routes[routeKey], args);

    return new Handlebars.SafeString(result);
});

/**
 * Handlebars "partial" helper
 * @example
 * {{partial this.partialName this}}
 */
Handlebars.registerHelper("partial", (name, context) => {
    const partial = Handlebars.partials[name];

    if (partial) {
        return new Handlebars.SafeString(partial(context));
    } else {
        console.error(`Handlebars "partial" helper unable to find partial "${name}"`);
    }
});

Handlebars.registerHelper("policyEditorResourceHelper", function () {
    let result = this.options.newPattern.replace("-*-", "̂");
    result = result.replace(/\*/g,
        '<input class="form-control" required type="text" value="*" placeholder="*" />');
    result = result.replace("̂",
        '<input class="form-control" required type="text" value="-*-" placeholder="-*-" pattern="[^/]+" />');

    return new Handlebars.SafeString(result);
});

Handlebars.registerHelper("debug", function () {
    console.warn("[handlebars] debug. Value of `this`");
    console.warn(this);
});

Handlebars.registerHelper("ternary", (testExpression, yes, no) => {
    return testExpression ? yes : no;
});

// Registering global mixins
_.mixin({

    /**
     * findByValues takes a collection and returns a subset made up of objects where the given property name
     * matches a value in the list.
     * @returns {Array} subset of made up of {Object} where there is no match between the given property name and
     *                  the values in the list.
     * @example
     *
     *    var collections = [
     *        {id: 1, stack: 'am'},
     *        {id: 2, stack: 'dj'},
     *        {id: 3, stack: 'idm'},
     *        {id: 4, stack: 'api'},
     *        {id: 5, stack: 'rest'}
     *    ];
     *
     *    var filtered = _.findByValues(collections, "id", [1,3,4]);
     *
     *    filtered = [
     *        {id: 1, stack: 'am'},
     *        {id: 3, stack: 'idm'},
     *        {id: 4, stack: 'api'}
     *    ]
     *
     */
    "findByValues" (collection, property, values) {
        return _.filter(collection, (item) => {
            return _.contains(values, item[property]);
        });
    },

    /**
     * Returns subset array from a collection
     * @returns {Array} subset of made up of {Object} where there is no match between the given property name and
     *                  the values in the list.
     * @example
     *
     *    var filtered = _.removeByValues(collections, "id", [1,3,4]);
     *
     *    filtered = [
     *        {id: 2, stack: 'dj'},
     *        {id: 5, stack: 'rest'}
     *    ]
     *
     */
    "removeByValues" (collection, property, values) {
        return _.reject(collection, (item) => {
            return _.contains(values, item[property]);
        });
    },

    /**
     * isUrl checks to see if string is a valid URL
     * @returns {Boolean}
     */
    "isUrl" (string) {
        const regexp = /(http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
        return regexp.test(string);
    }

});

export default UIUtils;
