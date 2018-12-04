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
 * Copyright 2018 ForgeRock AS.
 */

import $ from "jquery";
import i18next from "i18next";

import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import RenameDeviceDialogTemplate from "templates/user/dashboard/authenticationDevices/RenameDeviceDialogTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import { update } from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/WebAuthnService";

export default function (device, renamedSuccessCallback) {
    const template = RenameDeviceDialogTemplate(device);

    BootstrapDialog.show({
        title: i18next.t("openam.authDevices.editDeviceDialog.title"),
        message: $(template),
        buttons: [{
            label: i18next.t("common.form.cancel"),
            action: (dialog) => {
                dialog.close();
            }
        },
        {
            label: i18next.t("common.form.save"),
            cssClass: "btn-primary",
            action (dialog) {
                const deviceName = dialog.$modalBody.find("#deviceName").val();

                update(device.uuid, { deviceName }).then(() => {
                    dialog.close();
                    renamedSuccessCallback();
                    Messages.addMessage({ message: i18next.t("config.messages.AppMessages.changesSaved") });
                }, (response) => {
                    Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                });
            }
        }]
    });
}
