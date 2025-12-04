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
 * Copyright 2011-2025 Ping Identity Corporation.
 */

import { bind, extend, has, isFunction, isString, noop, map } from "lodash";
import $ from "jquery";
import Backbone from "backbone";
import debug from "debug";

import { getTheme } from "org/forgerock/openam/ui/common/util/ThemeManager";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import DefaultBaseTemplate from "themes/default/templates/common/DefaultBaseTemplate";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import loadPartial from "org/forgerock/openam/ui/common/util/theme/loadPartial";
import loadTemplate from "org/forgerock/openam/ui/common/util/theme/loadTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import UIUtils from "org/forgerock/commons/ui/common/util/UIUtils";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";

/**
 * @exports org/forgerock/commons/ui/common/main/AbstractView
 */

/**
  Sets the enabled state of the submit button based on the validation status of the provided form
*/
function validationCompleted (formElement) {
    let button = formElement.find("input[type=submit]");

    if (!button.length) {
        button = formElement.find("#submit");
    }
    if (button.length) {
        button.prop("disabled", !ValidatorsManager.formValidated(formElement));
    }
}

export default Backbone.View.extend({
    /**
     * This params should be passed when creating new object, for example:
     * new View({el: "#someId", template: "templates/main.html"});
     */
    element: "#content",

    baseTemplate: DefaultBaseTemplate,

    /**
     * View mode: replace or append
     */
    mode: "replace",
    defaultEvents: {
        "validationSuccessful :input": "validationSuccessful",
        "validationReset :input": "validationSuccessful",
        "validationFailed :input": "validationFailed"
    },
    initialize () {
        this.data = this.data || {};
        extend(this.events, this.defaultEvents);
        this.delegateEvents();
        UIUtils.initHelpers();
    },

    /**
     * Change content of 'el' element with 'viewTpl',
     * which is compiled using 'data' attributes.
     */
    async parentRender (callback) {
        const logger = debug("forgerock:am:user:view:template");

        this.callback = callback;

        const needsNewBaseTemplate = () => {
            return (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate);
        };

        const theme = await getTheme();
        this.data.theme = theme;

        if (needsNewBaseTemplate()) {
            const baseTemplatePath = isString(this.baseTemplate)
                ? ` \`${this.baseTemplate}\` `
                : " ";
            logger(`Base template${baseTemplatePath}loading...`);

            this.baseTemplate = await loadTemplate(this.baseTemplate, theme.path);

            UIUtils.renderTemplate(
                this.baseTemplate,
                $("#wrapper"),
                extend({}, Configuration.globalData, this.data),
                undefined,
                "replace",
                needsNewBaseTemplate
            );

            logger(`Base template${baseTemplatePath}loaded.`);
        }

        this.loadTemplate();
    },

    async loadTemplate () {
        const self = this;

        const validateCurrent = function () {
            if (!has(self, "route")) {
                return true;
            } else if (!self.route.url.length && URIUtils.getCurrentFragment().replace(/^#/, "") === "") {
                return true;
            } else if (self.route === Router.configuration.routes.login) {
                /**
                     * Determines if the current route is a login route, in which case allow the route  to execute.
                     * This is due to OpenAM's requirement for two views rendering being rendered at the same time
                     * (an arbitrary view and a session expiry login dialog view layered above) where the route and
                     * the hash don't match.
                     */
                return true;
            } else {
                return URIUtils.getCurrentFragment().replace(/^#/, "").match(self.route.url);
            }
        };

        this.setElement($(this.element));
        this.$el.unbind();
        this.delegateEvents();

        if (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
            Configuration.setProperty("baseTemplate", this.baseTemplate);
            EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
        }

        const { path: themePath } = await getTheme();
        await Promise.all(map(this.partials, (path, name) => loadPartial(name, path, themePath)));

        const renderTemplate = (template) => {
            UIUtils.renderTemplate(
                template,
                self.$el,
                extend({}, Configuration.globalData, self.data),
                self.callback ? bind(self.callback, self) : noop(),
                self.mode,
                validateCurrent);
        };

        self.template = unwrapDefaultExport(self.template);

        /**
         * Some views do no define a template. UIUtils.renderTemplate handles this for us
         * so the `undefined` template is simply passed straight along to #renderTemplate.
         */
        if (isFunction(self.template) || !self.template) {
            renderTemplate(self.template);
        } else {
            this.loadThemedTemplate(self.template).then(renderTemplate);
        }
    },

    async loadThemedTemplate (path) {
        const { path: themePath } = await getTheme();

        return await loadTemplate(path, themePath);
    },

    rebind () {
        this.setElement($(this.element));
        this.$el.unbind();
        this.delegateEvents();
    },

    render (args, callback) {
        this.parentRender(callback);
    },

    /**
     * This is the default implementation of the function used to reflect that
     * a given field has passed validation. It is invoked via a the event system,
     * and can be overridden per-view as needed.
     */
    validationSuccessful (event) {
        const input = $(event.target);
        if (input.data()["bs.popover"]) {
            input.popover("destroy");
        }
        input.parents(".form-group").removeClass("has-feedback has-error");
        validationCompleted(input.closest("form"));
    },

    /**
     * This is the default implementation of the function used to reflect that
     * a given field has failed validation. It is invoked via a the event system,
     * and can be overridden per-view as needed.
     * @param {object} event The event.
     * @param {object} details - "failures" entry lists all messages (localized) associated with this validation
     *                           failure
     */
    validationFailed (event, details) {
        const input = $(event.target);
        input.parents(".form-group").addClass("has-feedback has-error");
        if (input.data()["bs.popover"]) {
            input.data("bs.popover").options.content = `<i class="fa fa-exclamation-circle"></i> ${
                details.failures.join('<br><i class="fa fa-exclamation-circle"></i> ')}`;
        } else {
            input.popover({
                validationMessage: details.failures,
                animation: false,
                content: `<i class="fa fa-exclamation-circle"></i> ${
                    details.failures.join('<br><i class="fa fa-exclamation-circle"></i> ')}`,
                trigger:"focus hover",
                placement:"top",
                html: "true",
                template: '<div class="popover popover-error help-block" role="tooltip">' +
                '<div class="arrow"></div><h3 class="popover-title"></h3>' +
                '<div class="popover-content"></div></div>'
            });
        }
        if (input.is(":focus")) {
            input.popover("show");
        }
        validationCompleted(input.closest("form"));
    },

    // legacy; needed here to prevent breakage of views which have an event registered for this function
    onValidate () {
        console.warn("Deprecated use of onValidate method; Change to validationSuccessful / validationFailed");
    }
});
