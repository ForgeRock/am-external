/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialogView"
], function($, BootstrapDialogView) {
    var ConfirmationDialog = BootstrapDialogView.extend({
        render: function(title, msg, actionName, okCallback) {
            this.setElement($('<div id="CommonConfirmationDialog"></div>'));
            this.title = title;
            this.message = msg;
            this.actions =  [
                {
                    label: $.t("common.form.cancel"),
                    action: function (dialogRef) {
                        dialogRef.close();
                    }
                },
                {
                    label: actionName,
                    cssClass: "btn-primary",
                    action: function (dialogRef) {
                        if (okCallback) {
                            okCallback();
                        }
                        dialogRef.close();
                    }
                }
            ];

            this.show();
        }
    });

    return new ConfirmationDialog();
});
