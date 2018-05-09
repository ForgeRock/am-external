/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "templates/user/dashboard/EditDeviceDialogTemplate"
], ($, BootstrapDialog, EditDeviceDialogTemplate) => {
    return function (uuid, device) {
        const data = {
            deviceName: device.deviceName,
            recoveryCodes: device.recoveryCodes
        };

        const tpl = EditDeviceDialogTemplate(data);
        BootstrapDialog.show({
            title: device.deviceName,
            message: $(tpl),
            cssClass: "device-details",
            buttons: [{
                label: $.t("common.form.close"),
                action: (dialog) => {
                    dialog.close();
                }
            }]
        });
    };
});
