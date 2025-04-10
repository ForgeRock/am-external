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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";
import form2js from "form2js/src/form2js";
import js2form from "form2js/src/js2form";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ChangesPending from "org/forgerock/commons/ui/common/components/ChangesPending";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import ConfirmPasswordDialog from "org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";

/**
 * Provides base functionality for all tabs within UserProfileView
 * @exports org/forgerock/commons/ui/user/profile/AbstractUserProfileTab
 */
const AbstractUserProfileTab = AbstractView.extend({
    noBaseTempate: true,
    events: {
        "click input[type=submit]": "formSubmit",
        "click input[type=reset]": "resetForm",
        "reload form": "reloadFormData",
        "change :input": "checkChanges"
    },

    /**
     * Attaches a ChangesPending instance within the view
     * Requires the presence of an element with the "changes-pending" class
     * Initializes with the current value from this.getFormContent()
     */
    initializeChangesPending () {
        this.changesPendingWidget = ChangesPending.watchChanges({
            element: this.$el.find(".changes-pending"),
            watchedObj: { subform: this.getFormContent() },
            watchedProperties: ["subform"],
            alertClass: "alert-warning alert-sm"
        });
    },

    /**
     * Works with form validators and changes pending widget to reflect the state of the
     * form as the user makes their edits.
     * @param {Event} event The event
     */
    checkChanges (event) {
        const target = $(event.target);
        const form = $(target.closest("form"));

        ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, () => {
            ValidatorsManager.validateAllFields(form);
        });

        target.trigger("validate");
        if (!target.attr("data-validation-dependents")) {
            this.changesPendingWidget.makeChanges({ subform: this.getFormContent() });
        }

        form.find("input[type='reset']").prop("disabled", false);
    },

    /**
     * Generic method for reading content from the view's form. Extend if necessary for more
     * complex form parsing needs.
     * @returns {object} The form content.
     */
    getFormContent () {
        return form2js(this.$el.find("form")[0], ".", false);
    },

    /**
     * Used for populating the form with a "clean" set of data, either when first rendered
     * or when the form is reset.
     * @param {object} userData User data
     * @param {boolean} [disableSubmit=false] Whether to disable the submit button on render
     */
    reloadFormData (userData, disableSubmit = false) {
        const form = this.$el.find("form");
        this.data.user = userData || this.data.user;
        js2form(form[0], this.data.user);
        $("input[type=password]", form).val("").attr("placeholder", $.t("common.form.passwordPlaceholder"));
        this.rebindValidators(disableSubmit);
        this.initializeChangesPending();
    },

    /**
     * Rebinds validators to user form.
     * @param {boolean} [disableSubmit=false] Whether to disable the submit button on render
     */
    rebindValidators (disableSubmit = false) {
        const form = this.$el.find("form");
        ValidatorsManager.clearValidators(form);
        ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, () => {
            form.find("input[type='reset']").prop("disabled", true);
            form.find("input[type='submit']").prop("disabled", disableSubmit);
        });
    },

    resetForm (event) {
        event.preventDefault();
        this.reloadFormData(null, true);
    },

    /**
     * Generic save method  - patch the user model with the local data and persist it
     * @param {object} formData User data
     */
    submit (formData) {
        Configuration.loggedUser.save(formData, { patch: true }).then(
            _.bind(function () {
                this.submitSuccess();
            }, this)
        );
    },

    /**
     * After a form is saved, reset the content with the most recent data for the user
     */
    submitSuccess () {
        this.data.user = Configuration.loggedUser.toJSON();
        this.reloadFormData(null, true);
        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
    },

    /**
     * Attempt to submit the form. If the form is invalid, it will fail. If the user
     * is changing a protected attribute, prompt them to first enter their old password.
     * Finally, attempt to actually submit the form data.
     * @param {Event} event The event
     */
    formSubmit (event) {
        event.preventDefault();
        event.stopPropagation();

        let changedProtected = [];
        const form = $(event.target).closest("form");
        const formData = this.getFormContent(form[0]);

        if (ValidatorsManager.formValidated(form)) {
            changedProtected = _.chain(Configuration.loggedUser.getProtectedAttributes())
                .filter((attr) => {
                    if (_.has(formData, attr)) {
                        if (_.isEmpty(Configuration.loggedUser.get(attr)) && _.isEmpty(formData[attr])) {
                            return false;
                        } else {
                            return !_.isEqual(Configuration.loggedUser.get(attr), formData[attr]);
                        }
                    } else {
                        return false;
                    }
                })
                .map((attr) => {
                    return this.$el.find(`label[for=input-${attr}]`).text();
                })
                .value();

            if (changedProtected.length === 0) {
                this.submit(formData);
            } else {
                ConfirmPasswordDialog.render(changedProtected, _.bind(function (currentPassword) {
                    Configuration.loggedUser.setCurrentPassword(currentPassword);
                    this.submit(formData);
                }, this));
            }
        }
    }

});

export default AbstractUserProfileTab;
