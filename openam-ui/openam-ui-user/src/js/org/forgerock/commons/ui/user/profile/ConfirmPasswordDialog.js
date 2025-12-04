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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

import $ from "jquery";

import BootstrapDialogView from "org/forgerock/commons/ui/common/components/BootstrapDialogView";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";
import ConfirmPasswordDialogTemplate from "themes/default/templates/user/ConfirmPasswordDialogTemplate";
const ConfirmPasswordDialog = BootstrapDialogView.extend({
    contentTemplate: ConfirmPasswordDialogTemplate,
    events: {
        "onValidate": "onValidate",
        "submit #confirmPasswordForm": "submitForm",
        "change #currentPassword": "validateForm",
        "keyup #currentPassword": "validateForm"
    },
    submitForm (e) {
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
    title () { return $.t("common.user.confirmPassword"); },
    actions: [{
        id: "btnUpdate",
        label () { return $.t("common.form.update"); },
        cssClass: "btn-primary",
        disabled: true,
        action (dialog) {
            dialog.options.submitForm();
        }
    }],
    onshown (dialog) {
        this.dialog = dialog;
        this.element = dialog.$modal;
        this.rebind();
        dialog.$modal.find(".modal-body :input:first").val("");
        ValidatorsManager.bindValidators(dialog.$modal);
    },
    render (changedProtected, completedCallback) {
        this.data.changedProtected = changedProtected;
        this.completedCallback = completedCallback;
        this.show();
    },
    validateForm () {
        if (this.$el.find("#currentPassword").val().length === 0) {
            this.$el.find("#btnUpdate").prop("disabled", true).addClass("disabled");
        } else {
            this.$el.find("#btnUpdate").prop("disabled", false).removeClass("disabled");
        }
    }
});

export default new ConfirmPasswordDialog();
