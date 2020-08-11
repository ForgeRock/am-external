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
 * Copyright 2016-2019 ForgeRock AS.
 */

import $ from "jquery";
import _ from "lodash";
import i18next from "i18next";

import {
    getAll as getAllPush,
    remove as removePush,
    isChecked as isPushChecked,
    update as savePush
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/PushDeviceService";
import {
    getAll as getAllWebAuthn,
    remove as removeWebAuthn
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/WebAuthnService";
import {
    getAll as getAllOath,
    remove as removeOath,
    isChecked as isOathChecked,
    update as saveOath
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/OathDeviceService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import renameDeviceDialog from
    "org/forgerock/openam/ui/user/dashboard/views/authenticationDevices/renameDeviceDialog";
import multiFactorAuthDialog from
    "org/forgerock/openam/ui/user/dashboard/views/authenticationDevices/multiFactorAuthDialog";

/*
 * There are three types of Authentication devices, Push and Oath (which both behave the same in the UI), and WebAuthN.
 * Each type has three states:
 * 1: Authenticated - User has authenticated with a device of this type.
 * 2: Active - User has a device of this type but has not authenticated with it.
 * 3: None - User does not have a device of this type.
 *
 * Push and OATH devices:
 * There can only be a maximum of one (of each) Push and OATH.
 * Authenticated - The user will be able to change the MFA settings and delete the device. Once deleted they wll still
 * be able to change the settings.
 * Active/None - The user will still be premitted to view the current MFA settings for this type, but will not be able
 * to change or delete anything.
 *
 * WebAuthN devices
 * There is no limit to the number of WebAuthN devices.
 * Authenticated - The user will be able to change the name and delete the device.
 * Active - The devices of this type will be displayed but they cannot be deleted or renamed.
 * None - No actions can be carried out on the device and nothing will be displayed.
 */
class AuthenticationDevicesPanel extends AbstractView {
    constructor () {
        super();
        this.template = "user/dashboard/authenticationDevices/AuthenticationDevicesTemplate";
        this.noBaseTemplate = true;
        this.element = "#authenticationDevices";
        this.events = {
            "click [data-delete]":  "handleDelete",
            "click [data-edit]" : "handleEdit"
        };
    }
    handleDelete (event) {
        event.preventDefault();
        const { deviceType, deviceUuid } = $(event.currentTarget).data();
        let deleteFunc;

        switch (deviceType) {
            case "oath": deleteFunc = removeOath; break;
            case "push": deleteFunc = removePush; break;
            case "webauthn": deleteFunc = removeWebAuthn; break;
        }

        const username = Configuration.loggedUser.get("username");

        deleteFunc(username, deviceUuid).then(() => {
            this.render();
        });
    }

    handleEdit (event) {
        event.preventDefault();
        const { deviceType, deviceManagementStatus, deviceName, deviceUuid } = $(event.currentTarget).data();

        switch (deviceType) {
            case "oath":
                multiFactorAuthDialog({
                    deviceUuid,
                    isChecked: isOathChecked,
                    isReadOnly: !deviceManagementStatus,
                    save: saveOath,
                    title: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.title"),
                    label: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.label"),
                    descriptions: {
                        auth: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.description.auth"),
                        noAuth: i18next.t("openam.authDevices.common.noAuth", { deviceName }),
                        noDevice: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.description.noDevice")
                    }
                });
                break;
            case "push":
                multiFactorAuthDialog({
                    deviceUuid,
                    isChecked: isPushChecked,
                    isReadOnly: !deviceManagementStatus,
                    save: savePush,
                    title: i18next.t("openam.authDevices.multiFactorAuthDialog.push.title"),
                    label: i18next.t("openam.authDevices.multiFactorAuthDialog.push.label"),
                    descriptions: {
                        auth: i18next.t("openam.authDevices.multiFactorAuthDialog.push.description.auth"),
                        noAuth: i18next.t("openam.authDevices.common.noAuth", { deviceName }),
                        noDevice: i18next.t("openam.authDevices.multiFactorAuthDialog.push.description.noDevice")
                    }
                });
                break;
            case "webauthn":
                renameDeviceDialog({
                    deviceName,
                    deviceUuid,
                    isReadOnly: !deviceManagementStatus
                }, () => {
                    this.render();
                });
                break;
        }
    }

    render () {
        const username = Configuration.loggedUser.get("username");
        Promise.all([getAllOath(username), getAllPush(username), getAllWebAuthn(username)])
            .then(([allOath, allPush, allWebAuthn]) => {
                const oathCard = {
                    icon: "clock-o",
                    type: "oath",
                    title: i18next.t("openam.authDevices.cards.oath"),
                    deviceName: i18next.t("openam.authDevices.cards.noDevice"),
                    ..._.get(allOath, "result[0]", {})
                };

                const pushCard = {
                    icon: "shield",
                    type: "push",
                    title: i18next.t("openam.authDevices.cards.push"),
                    deviceName: i18next.t("openam.authDevices.cards.noDevice"),
                    ..._.get(allPush, "result[0]", {})
                };

                const webAuthnCards = [
                    ..._.map(allWebAuthn.result, (device) => ({
                        icon: "shield",
                        type: "webauthn",
                        title: device.deviceName,
                        ...device
                    }))
                ];

                this.data.devices = [oathCard, pushCard, ...webAuthnCards];
                this.parentRender(() => {
                    this.$el.find('[data-toggle="popover"]').popover();
                });
            });
    }
}

export default new AuthenticationDevicesPanel();
