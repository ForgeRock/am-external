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
 * Copyright 2016-2018 ForgeRock AS.
 */

import $ from "jquery";
import _ from "lodash";
import i18next from "i18next";

import {
    getAll as getAllPush,
    remove as removePush,
    isMultiFactorAuthEnabled as isPushMultiFactorAuthEnabled,
    setMultiFactorAuth as setPushMultiFactorAuth
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/PushDeviceService";
import {
    getAll as getAllWebAuthn,
    remove as removeWebAuthn
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/WebAuthnService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import {
    getAll as getAllOath,
    remove as removeOath,
    isMultiFactorAuthEnabled as isOathMultiFactorAuthEnabled,
    setMultiFactorAuth as setOathMultiFactorAuth
} from "org/forgerock/openam/ui/user/dashboard/services/authenticationDevices/OathDeviceService";
import renameDeviceDialog from
    "org/forgerock/openam/ui/user/dashboard/views/authenticationDevices/renameDeviceDialog";
import multiFactorAuthDialog from
    "org/forgerock/openam/ui/user/dashboard/views/authenticationDevices/multiFactorAuthDialog";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

const getAttributeFromElement = (element, attribute) => $(element).closest(`div[${attribute}]`).attr(attribute);
const getUUIDFromElement = (element) => getAttributeFromElement(element, "data-device-uuid");
const getTypeFromElement = (element) => getAttributeFromElement(element, "data-device-type");

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

        const uuid = getUUIDFromElement(event.currentTarget);
        const type = getTypeFromElement(event.currentTarget);
        let deleteFunc;

        switch (type) {
            case "oath": deleteFunc = removeOath; break;
            case "push": deleteFunc = removePush; break;
            case "webauthn": deleteFunc = removeWebAuthn; break;
        }

        deleteFunc(uuid).then(() => {
            this.render();
        }, (response) => {
            Messages.addMessage({ type: Messages.TYPE_DANGER, response });
        });
    }

    handleEdit (event) {
        event.preventDefault();

        const uuid = getUUIDFromElement(event.currentTarget);
        const device = _.find(this.data.devices, { uuid });
        const type = getTypeFromElement(event.currentTarget);

        switch (type) {
            case "oath":
                multiFactorAuthDialog({
                    isMultiFactorAuthEnabled: isOathMultiFactorAuthEnabled,
                    setMultiFactorAuth: setOathMultiFactorAuth,
                    title: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.title"),
                    label: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.label"),
                    description: i18next.t("openam.authDevices.multiFactorAuthDialog.oath.description")
                });
                break;
            case "push":
                multiFactorAuthDialog({
                    isMultiFactorAuthEnabled: isPushMultiFactorAuthEnabled,
                    setMultiFactorAuth: setPushMultiFactorAuth,
                    title: i18next.t("openam.authDevices.multiFactorAuthDialog.push.title"),
                    label: i18next.t("openam.authDevices.multiFactorAuthDialog.push.label"),
                    description: i18next.t("openam.authDevices.multiFactorAuthDialog.push.description")
                });
                break;
            case "webauthn":
                renameDeviceDialog(device, () => {
                    this.render();
                });
                break;
        }
    }

    render () {
        /*
         * There are three potential responses when making Oath and Push rest-calls.
         * 1: A populated array indicates there is a device present (There can only one device for each).
         * 2: An empty array indicates the user has signed in securely and has access to this endpoint, but does not
         * have any devices registered. In this situation the user still needs to be able to change the mfa setting.
         * 3: A 403 indicates the user has not signed in securely and does not have access to this endpoint.
         */
        const promisesCatcher = (promises) => promises.map((promise) => promise.catch(() => ({})));
        /* FIXME AME-17153 Promise.all(...).then() is used here in place of finally(). This is due to a bug in babel
         * https://github.com/babel/babel/issues/8297 where finally() is not working in Firefox. The workaround is to
         * use .then() and a promise catcher which sends both resolved and rejected promises to the resolved function.
        */
        Promise.all(promisesCatcher([
            getAllOath(), getAllPush(), getAllWebAuthn()
        ])).then(([oathResponse, pushResponse, webAuthnResponse]) => {
            const oathDevice = [];
            const pushDevice = [];
            const webAuthnDevices = [];

            if (oathResponse.result) {
                const device = {
                    deviceName: i18next.t("openam.authDevices.ghostCards.oath"),
                    type: "oath",
                    icon: "clock-o",
                    ..._.get(oathResponse, "result[0]", {})
                };

                oathDevice.push(device);
            }

            if (pushResponse.result) {
                const device = {
                    deviceName: i18next.t("openam.authDevices.ghostCards.push"),
                    type: "push",
                    icon: "bell-o",
                    ..._.get(pushResponse, "result[0]", {})
                };
                pushDevice.push(device);
            }

            if (!_.isEmpty(webAuthnResponse.result)) {
                webAuthnDevices.push(
                    ..._.map(webAuthnResponse.result, (device) => ({ ...device, type: "webauthn", icon: "shield" }))
                );
            }

            this.data.devices = [...oathDevice, ...pushDevice, ...webAuthnDevices];
            this.parentRender(() => {
                this.$el.find('[data-toggle="popover"]').popover();
            });
        });
    }
}

export default new AuthenticationDevicesPanel();
