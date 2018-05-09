/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialogView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "templates/user/ConfirmPasswordDialogTemplate"
], function($, BootstrapDialogView, ValidatorsManager, ConfirmPasswordDialogTemplate) {
    var ConfirmPasswordDialog = BootstrapDialogView.extend({
        contentTemplate: ConfirmPasswordDialogTemplate,
        events: {
            "onValidate": "onValidate",
            "submit #confirmPasswordForm": "submitForm",
            "change #currentPassword": "validateForm",
            "keyup #currentPassword": "validateForm"
        },
        submitForm: function (e) {
            if (e) {
                e.preventDefault();
            }

            if (this.completedCallback) {
                this.completedCallback(this.$el.find("#currentPassword").val());
            }
            this.dialog.close();
        },
        errorsHandlers: {
            "Bad Request": { status: "400" }
        },
        title: function(){ return $.t("common.user.confirmPassword"); },
        actions: [{
            id: "btnUpdate",
            label: function(){ return $.t("common.form.update"); },
            cssClass: "btn-primary",
            disabled: true,
            action: function(dialog) {
                dialog.options.submitForm();
            }
        }],
        onshown: function(dialog){
            this.dialog = dialog;
            this.element = dialog.$modal;
            this.rebind();
            dialog.$modal.find(".modal-body :input:first").val("");
            ValidatorsManager.bindValidators(dialog.$modal);
        },
        render: function(changedProtected, completedCallback) {
            this.data.changedProtected = changedProtected;
            this.completedCallback = completedCallback;
            this.show();
        },
        validateForm: function(){
            if (this.$el.find("#currentPassword").val().length === 0) {
                this.$el.find("#btnUpdate").prop("disabled", true).addClass("disabled");
            } else {
                this.$el.find("#btnUpdate").prop("disabled", false).removeClass("disabled");
            }
        }
    });

    return new ConfirmPasswordDialog();
});
