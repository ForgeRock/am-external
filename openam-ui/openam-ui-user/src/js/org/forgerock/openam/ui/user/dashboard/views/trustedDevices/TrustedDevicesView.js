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
 * Copyright 2015-2021 ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import TrustedDevicesService from "org/forgerock/openam/ui/user/dashboard/services/TrustedDevicesService";
import TrustedDeviceProfileService from "org/forgerock/openam/ui/user/dashboard/services/TrustedDeviceProfileService";
import moment from "moment";
import UAParser from "ua-parser-js";
import { get } from "lodash";

const TrustedDevices = AbstractView.extend({
    template: "user/dashboard/trustedDevices/TrustedDevicesTemplate",
    noBaseTemplate: true,
    element: "#myTrustedDevicesSection",
    events: {
        "click  a.deleteDevice" : "deleteDevice"
    },

    render () {
        const self = this;

        Promise.all([TrustedDevicesService.getTrustedDevices(), TrustedDeviceProfileService.getTrustedDeviceProfiles()])
            .then(([devices, deviceProfiles]) => {
                const allDevices = self.formatDevices([...devices.result, ...deviceProfiles.result]);
                self.data.legacyOnly = (deviceProfiles.result.length === 0);
                self.data.devices = self.sortDevicesByDate(allDevices);

                self.parentRender(() => {
                    self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
                });
            });
    },
    sortDevicesByDate (devices) {
        return devices.sort((cur, next) => moment(next.lastSelectedDate).unix() - moment(cur.lastSelectedDate).unix());
    },
    formatDevices (devices) {
        devices.forEach((device) => {
            // If object has metadata property it is a deviceProfile and has
            // extra properties to format
            if (device.metadata) {
                const ua = get(device, "metadata.browser.userAgent", "");
                const { browser, os } = ua ? UAParser(ua) : { browser: {}, os: {} };
                const profileId = localStorage.getItem("profile-id");

                device.name = device.alias;
                device.cpu = `${get(device, "metadata.platform.platform", "")}`;
                device.os = (`${get(os, "name", "")} ${get(os, "version", "")}`).trim();
                device.browser = (`${get(browser, "name", "")} ${get(browser, "version", "")}`).trim();
                device.deviceType = get(os, "name", "").replace(/ /g, "").toLowerCase();
                device.isCurrent = device.identifier === profileId;
            }
            device.lastSelectedDate = moment(device.lastSelectedDate).format("D MMM YYYY, h:mm");
        });
        return devices;
    },
    deleteDevice (event) {
        event.preventDefault();
        const self = this;

        const id = event.currentTarget.getAttribute("data-id");
        const identifier = event.currentTarget.getAttribute("data-identifier");

        if (id) {
            TrustedDevicesService.deleteTrustedDevice(id).then(() => {
                console.log("Deleted trusted device");
                self.render();
            }, () => {
                console.error("Failed to delete trusted device");
            });
        } else {
            TrustedDeviceProfileService.deleteTrustedDeviceProfile(identifier).then(() => {
                console.log("Deleted trusted device profile");
                self.render();
            }, () => {
                console.error("Failed to delete trusted device profile");
            });
        }
    }
});

export default new TrustedDevices();
