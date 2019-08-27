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
 * Copyright 2015-2019 ForgeRock AS.
 */

import $ from "jquery";
import i18next from "i18next";

import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import MultiFactorAuthDialogTemplate
    from "templates/user/dashboard/authenticationDevices/MultiFactorAuthDialogTemplate";

const getDescription = (isReadOnly, deviceUuid, descriptions) => {
    if (isReadOnly) {
        return deviceUuid ? descriptions.noAuth : descriptions.noDevice;
    } else {
        return descriptions.auth;
    }
};

const showDialog = (template, isReadOnly, title, username, save) => {
    const cssClass = isReadOnly ? "btn-primary disabled" : "btn-primary";
    BootstrapDialog.show({
        buttons: [{
            id: "modalCancelBtn",
            action (dialog) {
                dialog.close();
            },
            label: i18next.t("common.form.cancel")
        }, {
            action (dialog) {
                const toggle = dialog.$modalBody.find("#multiFactorAuth");
                const cancelBtn = dialog.getButton("modalCancelBtn");
                cancelBtn.disable();
                this.disable();
                toggle.prop("disabled", true);
                dialog.setClosable(false);
                const isChecked = toggle.is(":checked");
                save(username, isChecked).then(() => {
                    dialog.close();
                    Messages.addMessage({ message: i18next.t("config.messages.AppMessages.changesSaved") });
                }, () => {
                    cancelBtn.enable();
                    this.enable();
                    toggle.prop("disabled", false);
                    dialog.setClosable(true);
                });
            },
            cssClass,
            disabled: isReadOnly,
            label: i18next.t("common.form.save")
        }],
        cssClass: "devices-settings",
        message: $(template),
        title
    });
};

export default function multiFactorAuthDialog (
    { isChecked, isReadOnly, title, label, descriptions, deviceUuid, save }) {
    const username = Configuration.loggedUser.get("username");

    isChecked(username).then((isChecked) => {
        const description = getDescription(isReadOnly, deviceUuid, descriptions);
        const template = MultiFactorAuthDialogTemplate({ isChecked, isReadOnly, label, description });

        showDialog(template, isReadOnly, title, username, save);
    });
}
