/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "i18next",
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/user/dashboard/services/DeviceManagementService",
    "templates/user/dashboard/DevicesSettingsDialogTemplate"
], (i18next, $, _, BootstrapDialog, Messages, DeviceManagementService, DevicesSettingsDialogTemplate) => {
    function closeDialog (dialog) {
        dialog.close();
    }

    return function () {
        const template = DevicesSettingsDialogTemplate;
        let authSkip = DeviceManagementService.checkDevicesOathSkippable();

        BootstrapDialog.show({
            title: $.t("openam.authDevices.devicesSettingDialog.title"),
            cssClass: "devices-settings",
            message: $("<div></div>"),
            buttons: [{
                label: $.t("common.form.cancel"),
                action: closeDialog
            }, {
                label: $.t("common.form.save"),
                cssClass: "btn-primary",
                action (dialog) {
                    authSkip = !dialog.$modalBody.find("#oathStatus").is(":checked");
                    DeviceManagementService.setDevicesOathSkippable(authSkip).then(() => {
                        dialog.close();
                        Messages.addMessage({ message: i18next.t("config.messages.AppMessages.changesSaved") });
                    }, (response) => {
                        Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                    });
                }
            }
            ],
            onshown (dialog) {
                $.when(authSkip).then((skip) => {
                    return template({ authNeeded: !skip });
                }, (response) => {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response
                    });
                }).then((tpl) => {
                    dialog.$modalBody.append(tpl);
                });
            }
        });
    };
});
