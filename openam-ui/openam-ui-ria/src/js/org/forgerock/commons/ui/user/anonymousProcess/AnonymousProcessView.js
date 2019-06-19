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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";
import form2js from "form2js/src/form2js";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AnonymousProcessDelegate from "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";

/**
 * Given a position in the DOM, look for children elements which comprise a
 * boolean expression. Using all of those found with content, return a filter
 * string which represents them.
 * @param {HTMLElement} basicNode HTML Element to walk from
 * @returns {string} Filter string
 */
function walkTreeForFilterStrings (basicNode) {
    const node = $(basicNode);
    let groupValues;

    if (node.hasClass("filter-value") && node.val().length > 0) {
        return `${node.attr("name")} eq "${node.val().replace('"', '\\"')}"`;
    } else if (node.hasClass("filter-group")) {
        groupValues = _.chain(node.find(">.form-group>.filter-value, >.filter-group"))
            .map(walkTreeForFilterStrings)
            .filter((value) => {
                return value.length > 0;
            })
            .value();

        if (groupValues.length === 0) {
            return "";
        } else if (groupValues.length === 1) {
            return groupValues[0];
        }

        if (node.hasClass("filter-or")) {
            return `(${groupValues.join(" OR ")})`;
        } else {
            return `(${groupValues.join(" AND ")})`;
        }
    } else {
        return "";
    }
}

/**
 * @exports org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView
 *
 * To use this generic object, it is required that two properties be defined prior to initialization:
 * "processType" - the Anonymous Process type registered on the backend under the selfservice context
 * "i18nBase" - the base of the translation file that contains the entries specific to this instance
 *
 * Example:
 *
 *    new AnonymousProcessView.extend({
 *         processType: "reset",
 *         i18nBase: "common.user.passwordReset"
 *     });
 */

const AnonymousProcessView = AbstractView.extend({
    baseTemplate: "user/AnonymousProcessBaseTemplate",
    template: "user/AnonymousProcessWrapper",
    events: {
        "submit form": "formSubmit",
        "click #restart": "restartProcess",
        "change #userQuery :input": "buildQueryFilter",
        "keyup #userQuery :input": "buildQueryFilter",
        "customValidate #userQuery": "validateForm"
    },
    data: {
        i18n: {}
    },
    // included as part of the AnonymousProcessView object for test purposes
    walkTreeForFilterStrings,

    getFormContent () {
        if (this.$el.find("form").attr("id") === "userQuery") {
            return {
                queryFilter: this.$el.find(":input[name=queryFilter]").val()
            };
        } else {
            return form2js($(this.element).find("form")[0]);
        }
    },

    formSubmit (event) {
        const formContent = this.getFormContent();

        event.preventDefault();

        this.delegate.submit(formContent).then(
            _.bind(this.renderProcessState, this),
            _.bind(this.renderProcessState, this)
        );
    },

    parentRender () {
        AbstractView.prototype.parentRender.call(this, _.bind(function () {
            this.delegate.start().then(_.bind(this.renderProcessState, this));
        }, this));
    },

    renderResponse (response) {
        AbstractView.prototype.parentRender.call(this, _.bind(function () {
            this.renderProcessState(response);
        }, this));
    },

    setDelegate (endpoint, token, additional) {
        this.delegate = new AnonymousProcessDelegate(endpoint, token, additional);
    },

    submitDelegate (params, onSubmit) {
        this.delegate.submit(_.omit(params, "token")).always(onSubmit);
    },

    setTranslationBase () {
        _.each(
            ["title", "completed", "failed", "tryAgain", "return"],
            _.bind(function (key) {
                this.data.i18n[key] = `${this.i18nBase}.${key}`;
            }, this)
        );
    },

    restartProcess () {},

    attemptCustomTemplate (stateData, response, processStatePromise) {
        this.loadThemedTemplate(`user/process/${this.processType}/${response.type}-${response.tag}`)
            .then((template) => {
                const renderedTemplate = template(stateData);
                processStatePromise.resolve(renderedTemplate);
            }, () => {
                this.loadGenericTemplate(stateData, response, processStatePromise);
            });
    },

    loadGenericTemplate (stateData, response, processStatePromise) {
        const templatePage = _.has(response, "requirements") ? "GenericInputForm" : "GenericEndPage";

        this.loadThemedTemplate(`user/process/${templatePage}`)
            .then((template) => {
                const renderedTemplate = template(stateData);
                processStatePromise.resolve(renderedTemplate);
            });
    },

    renderProcessState (response) {
        const processStatePromise = $.Deferred();

        if (_.has(response, "requirements")) {
            this.stateData = _.extend({
                requirements: response.requirements
            }, this.data);
        } else {
            this.stateData = _.extend({
                status: response.status,
                additions: response.additions
            }, this.data);
        }

        if (_.has(response, "type") && _.has(response, "tag")) {
            this.attemptCustomTemplate(this.stateData, response, processStatePromise);
        } else {
            this.loadGenericTemplate(this.stateData, response, processStatePromise);
        }

        return processStatePromise.then(_.bind(function (content) {
            this.$el.find("#processContent").html(content);
            ValidatorsManager.bindValidators(this.$el, this.baseEntity, _.bind(function () {
                ValidatorsManager.validateAllFields(this.$el);
            }, this));
        }, this));
    },

    buildQueryFilter () {
        this.$el.find(":input[name=queryFilter]")
            .val(walkTreeForFilterStrings(this.$el.find("#filterContainer")));
        this.validateForm();
    },
    validateForm () {
        const button = this.$el.find("input[type=submit]");
        let incompleteAndGroup = false;

        // there has to be some value in the queryFilter or the whole thing is invalid
        if (this.$el.find(":input[name=queryFilter]").val().length === 0) {
            button.prop("disabled", true);
            return;
        }

        // filter-and groups must have each of their children filled-out
        this.$el.find(".filter-and").each(function () {
            // if there are any values at all specified for this "and" group...
            if (walkTreeForFilterStrings(this).length > 0) {
                // then we need to make sure that they are all populated
                incompleteAndGroup = !(
                    // check all direct filter-value fields for content
                    (_.reduce($(">.form-group>.filter-value", this), (state, field) => {
                        const hasValue = field.value.length > 0;
                        $(field).attr("data-validation-status", hasValue ? "ok" : "error");
                        return state && hasValue;
                    }, true) && // check all direct sub-groups for content
                    _.reduce($(">.filter-group", this), (state, subGroup) => {
                        return walkTreeForFilterStrings(subGroup).length > 0;
                    }, true))
                );
            } else {
                $(">.form-group>.filter-value", this).attr("data-validation-status", "ok");
            }
        });

        button.prop("disabled", incompleteAndGroup);
    }

});

export default AnonymousProcessView;
