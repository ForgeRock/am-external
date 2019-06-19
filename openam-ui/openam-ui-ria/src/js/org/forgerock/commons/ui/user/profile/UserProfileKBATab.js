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
import Handlebars from "handlebars-template-loader/runtime";
import js2form from "form2js/src/js2form";

import AbstractUserProfileTab from "org/forgerock/commons/ui/user/profile/AbstractUserProfileTab";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import KBADelegate from "KBADelegate";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";

/**
 * An instance of AbstractUserProfileTab, to be used with the UserProfileView when
 * KBA management is available for the end-user.
 * @exports org/forgerock/commons/ui/user/profile/AbstractUserProfileTab
 */
const UserProfileKBATab = AbstractUserProfileTab.extend({
    template : "user/UserProfileKBATab",
    events: _.extend({
        "change .kba-pair :input": "checkChanges",
        "keyup .kba-pair :input": "checkChanges",
        "mouseup .kba-pair select": "checkChanges",
        "click #provideAnother": "addKBAQuestion",
        "click .delete-KBA-question": "deleteKBAQuestion"
    }, AbstractUserProfileTab.prototype.events),
    partials: {
        "profile/_kbaItem": "profile/_kbaItem"
    },

    /**
     * Expected by all dynamic user profile tabs - returns a map of details necessary to render the nav tab
     * @returns {Object} Tab detail
     */
    getTabDetail () {
        return {
            "panelId": "userKBATab",
            "label": $.t("common.user.kba.securityQuestions")
        };
    },

    addKBAQuestion (e) {
        e.preventDefault();
        const kbaItems = this.$el.find("#kbaItems");
        const form = $(e.target).closest("form");
        const newIndex = kbaItems.find(">li").length;
        kbaItems.append(
            $("<li>").html(Handlebars.partials["profile/_kbaItem"]({
                questions: this.data.predefinedQuestions,
                index: newIndex,
                isNew: true
            }))
        );

        ValidatorsManager.bindValidators(this.$el.find("form"),
            Configuration.loggedUser.baseEntity,
            _.bind(function () {
                ValidatorsManager.validateAllFields(this.$el.find("form"));
            }, this)
        );

        $(form).find("input[type='reset']").prop("disabled", false);
    },

    deleteKBAQuestion (e) {
        const target = $(e.target);
        const form = target.closest("form");
        const kbaPair = target.closest(".kba-pair");

        e.preventDefault();

        if (kbaPair.attr("isNew") === "true") {
            kbaPair.parent("li").remove();
        } else {
            kbaPair.hide();
        }
        this.changesPendingWidget.makeChanges({ subform: this.getFormContent() });

        ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, () => {
            ValidatorsManager.validateAllFields(form);
        });

        $(form).find("input[type='reset']").prop("disabled", false);
    },

    checkChanges (e) {
        const target = $(e.target);
        const form = target.closest("form");
        const kbaPair = target.closest(".kba-pair");

        const answer = kbaPair.find(".answer :input");
        const attributeName = _.keys(form2js(e.target))[0];
        const currentKbaInfo = this.changesPendingWidget.data.watchedObj.subform[attributeName];
        const customQuestionContainer = kbaPair.find(".custom-question");
        const isKbaQuestion = target.hasClass("kba-questions");
        const predefinedQuestion = kbaPair.find(".kba-questions");

        let answerRequired = false;

        customQuestionContainer.toggleClass("hidden", predefinedQuestion.val() !== "custom");

        // below conditions check to see if a new KBA answer needs to be provided, or whether
        // it can stay unchanged
        if (currentKbaInfo && currentKbaInfo[kbaPair.attr("index")]) {
            if (predefinedQuestion.val() === "custom") {
                answerRequired = currentKbaInfo[kbaPair.attr("index")].customQuestion !==
                    customQuestionContainer.find(":input").val();
            } else {
                answerRequired = currentKbaInfo[kbaPair.attr("index")].questionId !== predefinedQuestion.val();
            }
        } else {
            answerRequired = true;
        }

        if (answerRequired) {
            answer.attr("data-validator", "required");
            answer.attr("placeholder", "");
        }

        // validate form only in case security question was selected
        if (!isKbaQuestion || (isKbaQuestion && (target.val() !== ""))) {
            ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, () => {
                ValidatorsManager.validateAllFields(form);
            });
        }

        if (!isKbaQuestion) {
            this.changesPendingWidget.makeChanges({ subform: this.getFormContent() });
        }

        $(form).find("input[type='reset']").prop("disabled", false);
    },
    /**
     * Overrides AbstractUserProfileTab implementation because saving KBA details
     * for the user is tricky, owing to the fact that patching multi-valued properties
     * is done differently in various contexts. The implementation in KBADelegate replaces
     * the whole kba property rather than the inner parts which might have changed.
     * @param {Object} formData Form data
     */
    submit (formData) {
        KBADelegate.saveInfo(formData).then(
            _.bind(function () {
                this.submitSuccess();
            }, this)
        );
    },

    /**
     * Overrides AbstractUserProfileTab implementation. Needs more complex logic
     * to handle the various conditions - particularly around the answers which may have
     * been previously available as hashed values, and must remain as they were if a new
     * answer was not provided.
     * @returns {*} Form content
     */
    getFormContent () {
        const form = this.$el.find("form")[0];
        const formContent = form2js(form, ".", false);
        // cannot rely upon a particular named field in the form content,
        // so apply the logic to all fields found in the form
        return _(formContent)
            .map((value, key) => {
                if (_.isArray(value)) {
                    return [
                        key,
                        _(value)
                            .map((kbaPair, index) => {
                                const newPair = {};

                                // deleted pairs will be hidden
                                if ($(form).is(":visible") &&
                                    !$(form).find(`.kba-pair[index=${index}]:visible`).length) {
                                    // express their removal via an explicit undefined value in that position
                                    return undefined;
                                }

                                if (kbaPair.answer && kbaPair.answer.length) {
                                    newPair.answer = kbaPair.answer;
                                } else if (this.data.user[key] && _.isObject(this.data.user[key][index])) {
                                    newPair.answer = this.data.user[key][index].answer;
                                }

                                if (kbaPair.questionId === "custom") {
                                    newPair.customQuestion = kbaPair.customQuestion;
                                } else {
                                    newPair.questionId = kbaPair.questionId;
                                }
                                return newPair;
                            })
                            .compact()
                            .value()
                    ];
                } else {
                    return [key, value];
                }
            })
            .fromPairs()
            .value();
    },

    render (data, callback) {
        this.data = data;
        KBADelegate.getInfo().then(_.bind(function (response) {
            this.data.predefinedQuestions = _.map(response.questions, (value, key) => {
                return { "id" : key, "question" : value };
            });
            this.data.numberOfQuestions = response.minimumAnswersToDefine;

            this.parentRender(callback);
        }, this));
    },

    /**
     * Rerender the template and populate the form using the latest details from the user's
     * kba property. Relies upon the DOM structure in the template to determine which field
     * contains the kba list; by default, it's called "kbaInfo".
     */
    reloadFormData () {
        let form;

        this.parentRender(() => {
            form = this.$el.find("form")[0];
            js2form(form,
                // use the form structure to find out which fields are defined for the kba form...
                _(form2js(form, ".", false))
                    .map((value, key) => {
                        // omit the "answer" property from any array found there...
                        if (_.isArray(this.data.user[key])) {
                            return [
                                key,
                                _.map(this.data.user[key], (kbaPair) => {
                                    return _.omit(kbaPair, "answer");
                                })
                            ];
                        } else {
                            return [key, this.data.user[key]];
                        }
                    })
                    .fromPairs()
                    .value()
            );

            _.each($(".kba-questions", form), (kbaSelect) => {
                const customQuestionContainer = $(kbaSelect).closest(".kba-pair").find(".custom-question");
                const customQuestionValue = customQuestionContainer.find(":input").val();
                if (customQuestionValue === "") {
                    customQuestionContainer.toggleClass("hidden", true);
                } else {
                    $(kbaSelect).val("custom");
                    customQuestionContainer.removeClass("hidden");
                }
            });

            this.initializeChangesPending();
            $(form).find("input[type='reset']").prop("disabled", true);
            $(form).find("input[type='submit']").prop("disabled", true);
        });
    }
});

export default new UserProfileKBATab();
