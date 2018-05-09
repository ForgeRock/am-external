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

define([
    "jquery",
    "underscore",
    "handlebars-template-loader/runtime",
    "i18next",
    "bootstrap-dialog",
    "ThemeManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/ExternalLinks",
], ($, _, Handlebars, i18next, BootstrapDialog, ThemeManager, Router, ExternalLinks) => {
    /**
     * @exports org/forgerock/commons/ui/common/util/UIUtils
     */
    const obj = {
        configuration: {
            partialUrls: {
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

    obj.templates = {};

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
    obj.renderTemplate = function (templateUrl, el, data, callback, mode, validation) {
        let compileTemplate;
        if(templateUrl) {
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
     * Loads all the Handlebars partials defined in the "partialUrls" attribute of this module's configuration
     */
    obj.preloadInitialPartials = function () {
        ThemeManager.getTheme().then((theme) => {
            _.each(obj.configuration.partialUrls, (partialUrl, name) => {
                const registerPartial = (partial) => Handlebars.registerPartial(name, partial);
                const importDefaultPartial = (url) => import(`partials/${url}.html`);

                if (theme.path.length > 0) {
                    import(`themes/${theme.path}partials/${partialUrl}.html`).then(registerPartial, () => {
                        importDefaultPartial(partialUrl).then(registerPartial);
                    });
                } else {
                    importDefaultPartial(partialUrl).then(registerPartial);
                }
            });
        });
    };

    $.fn.emptySelect = function () {
        return this.each(function () {
            if (this.tagName === "SELECT") {
                this.options.length = 0;
            }
        });
    };

    $.fn.loadSelect = function (optionsDataArray) {
        return this.emptySelect().each(function () {
            if (this.tagName === "SELECT") {
                let i, option, selectElement = this;
                for (i = 0; i < optionsDataArray.length; i++) {
                    option = new Option(optionsDataArray[i].value, optionsDataArray[i].key);
                    selectElement.options[selectElement.options.length] = option;
                }
            }
        });
    };

    $.event.special.delayedkeyup = {
        setup () {
            $(this).bind("keyup", $.event.special.delayedkeyup.handler);
        },

        teardown () {
            $(this).unbind("keyup", $.event.special.delayedkeyup.handler);
        },

        handler (event) {
            let self = this, args = arguments;

            event.type = "delayedkeyup";

            $.doTimeout("delayedkeyup", 250, () => {
                $.event.handle.apply(self, args);
            });
        }
    };

    //map should have format key : value
    Handlebars.registerHelper("selectm", (map, elementName, selectedKey, selectedValue, multiple, height) => {
        let result, prePart, postPart, content = "", isSelected, entityName;

        prePart = "<select";

        if (elementName && _.isString(elementName)) {
            prePart += ` name="${elementName}"`;
        }

        if (multiple) {
            prePart += ' multiple="multiple"';
        }

        if (height) {
            prePart += ` style="height: ${height}px"`;
        }

        prePart += ">";

        postPart = "</select> ";

        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue)) {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else if (selectedKey && selectedKey === entityName) {
                isSelected = true;
            }

            if (isSelected) {
                content += `<option value="${entityName}" selected="true">${$.t(map[entityName])}</option>`;
            } else {
                content += `<option value="${entityName}">${$.t(map[entityName])}</option>`;
            }
        }

        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });

    /**
     * Use this helper around a basic select to automatically
     * mark the option corresponding to the provided value as selected.
     *
     * @example JS
     *  this.data.mimeType = "text/html";
     *
     * @example HTML
     *  <select>
     *      {{#staticSelect mimeType}}
     *      <option value="text/html">text/html</option>
     *      <option value="text/plain">text/plain</option>
     *      {{/staticSelect}}
     *  </select>
     */
    Handlebars.registerHelper("staticSelect", function (value, options) {
        const selected = $("<select />").html(options.fn(this));
        if (typeof value !== "undefined" && value !== null) {
            selected.find(`[value=\'${value.toString().replace("'", "\\'")}\']`).attr({ "selected":"selected" });
        }
        return selected.html();
    });

    Handlebars.registerHelper("select", (map, elementName, selectedKey, selectedValue, additionalParams) => {
        let result, prePart, postPart, content = "", isSelected, entityName, entityKey;

        if (map && _.isString(map)) {
            map = JSON.parse(map);
        }

        if (elementName && _.isString(elementName)) {
            prePart = `<select name="${elementName}" ${additionalParams}>`;
        } else {
            prePart = "<select>";
        }

        postPart = "</select> ";

        for (entityName in map) {
            isSelected = false;
            if (selectedValue && _.isString(selectedValue) && selectedValue !== "") {
                if (selectedValue === map[entityName]) {
                    isSelected = true;
                }
            } else if (selectedKey && selectedKey !== "" && selectedKey === entityName) {
                isSelected = true;
            }

            if (entityName === "__null") {
                entityKey = "";
            } else {
                entityKey = entityName;
            }

            if (isSelected) {
                content += `<option value="${entityKey}" selected="true">${$.t(map[entityName])}</option>`;
            } else {
                content += `<option value="${entityKey}">${$.t(map[entityName])}</option>`;
            }
        }

        result = prePart + content + postPart;
        return new Handlebars.SafeString(result);
    });

    Handlebars.registerHelper("p", (countValue, options) => {
        let params, result;
        params = { count: countValue };
        result = i18next.t(options.hash.key, params);
        return new Handlebars.SafeString(result);
    });

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

    Handlebars.registerHelper("checkbox", (map, name) => {
        let ret = `<div class='checkboxList' id='${name}'><ol>`, idx,
            sortedMap = _.chain(map)
                .pairs()
                .sortBy((arr) => { return arr[1]; })
                .value();

        for (idx = 0; idx < sortedMap.length; idx++) {
            ret += `<li><input type="checkbox" name="${name}" value="${sortedMap[idx][0]}" id="${name}_${
                encodeURIComponent(sortedMap[idx][0])}"><label for="${name}_${
                encodeURIComponent(sortedMap[idx][0])}">${sortedMap[idx][1]}</label></li>`;
        }

        ret += "</ol></div>";

        return new Handlebars.SafeString(ret);
    });

    Handlebars.registerHelper("siteImages", (images) => {
        let ret = "", i;

        for (i = 0; i < images.length; i++) {
            ret += `<img class="item" src="${
                encodeURI(images[i])}" data-site-image="${
                encodeURI(images[i])}" />`;
        }

        return new Handlebars.SafeString(ret);
    });

    Handlebars.registerHelper("each_with_index", (array, fn) => {
        let buffer = "",
            item,
            k = 0,
            i = 0,
            j = 0;

        for (i = 0, j = array.length; i < j; i++) {
            if (array[i]) {
                item = {};
                item.value = array[i];

                // stick an index property onto the item, starting with 0
                item.index = k;

                item.first = (k === 0);
                item.last = (k === array.length);

                // show the inside of the block
                buffer += fn.fn(item);

                k++;
            }
        }

        // return the finished buffer
        return buffer;
    });

    Handlebars.registerHelper("camelCaseToTitle", (string) => {
        const newString = string.replace(/([a-z])([A-Z])/g, "$1 $2");
        return new Handlebars.SafeString(newString[0].toUpperCase() + newString.slice(1));
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

        if (!partial) {
            console.error(`Handlebars "partial" helper unable to find partial "${name}"`);
        } else {
            return new Handlebars.SafeString(partial(context));
        }
    });

    Handlebars.registerHelper("externalLink", (key) => {
        return _.get(ExternalLinks, key, "");
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

    obj.loadSelectOptions = function (data, el, empty, callback) {
        if (empty === undefined || empty === true) {
            data = [{
                "key" : "",
                "value" : $.t("common.form.pleaseSelect")
            }].concat(data);
        }

        el.loadSelect(data);

        if (callback) {
            callback(data);
        }
    };

    //This function exists to catch any legacy jqConfirms.
    //Once completly updated across the applications this function can be removed.
    obj.jqConfirm = function (message, confirmCallback) {
        this.confirmDialog(message, "default", confirmCallback);
    };

    /**
     * @param {string} message The text provided in the main body of the dialog
     * @param {string} type The type of dialog to display
     * default
     * info
     * primary
     * success
     * warning
     * danger
     * @param {Function} confirmCallback Fired when the confirm button is clicked
     * @param {object} overrides object containing overrides for dialog title, button names, and cancel button callback
     * example : {
     *     title : "Save Changes?",
     *     okText : "Save Changes",
     *     cancelText : "Discard",
     *     cancelCallback: () => {
     *        //cool stuff
     *     }
     * }
     *
     * @example
     *  UIUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDelete"), "danger",s _.bind(function(){
     *          //Useful stuff here
     *      }, this),
     *      { title: "Some Title Text",
     *        okText: "Text for OK button",
     *        cancelText: "Text for Cancel button",
     *        cancelCallback : function () { console.log("Cancelled"; }
     *      }
     *  });
     */
    obj.confirmDialog = function (message, type, confirmCallback, overrides) {
        overrides = overrides || {};

        let btnType = `btn-${type}`;

        if (type === "default") {
            btnType = "btn-primary";
        }

        BootstrapDialog.show({
            title: overrides.title || $.t("common.form.confirm"),
            type: `type-${type}`,
            message,
            id: "frConfirmationDialog",
            buttons: [
                {
                    label: overrides.cancelText || $.t("common.form.cancel"),
                    id: "frConfirmationDialogBtnClose",
                    action (dialog) {
                        if (overrides.cancelCallback) {
                            overrides.cancelCallback();
                        }
                        dialog.close();
                    }
                },
                {
                    label: overrides.okText || $.t("common.form.ok"),
                    cssClass: btnType,
                    id: "frConfirmationDialogBtnOk",
                    action (dialog) {
                        if (confirmCallback) {
                            confirmCallback();
                        }
                        dialog.close();
                    }
                }
            ]
        });
    };

    obj.responseMessageMatch = function (error, string) {
        const responseMessage = JSON.parse(error).message;
        return responseMessage.indexOf(string) > -1;
    };

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

    return obj;
});
