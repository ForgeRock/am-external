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
 * Copyright 2015-2018 ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import TrustedDevicesService from "org/forgerock/openam/ui/user/dashboard/services/TrustedDevicesService";

const TrustedDevices = AbstractView.extend({
    template: "user/dashboard/trustedDevices/TrustedDevicesTemplate",
    noBaseTemplate: true,
    element: "#myTrustedDevicesSection",
    events: {
        "click  a.deleteDevice" : "deleteDevice"
    },

    render () {
        var self = this;

        TrustedDevicesService.getTrustedDevices().then((data) => {
            self.data.devices = data.result;
            self.parentRender(() => {
                self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
            });
        });
    },

    deleteDevice (event) {
        event.preventDefault();
        var self = this;

        TrustedDevicesService.deleteTrustedDevice(event.currentTarget.id).then(() => {
            console.log("Deleted trusted device");
            self.render();
        }, () => {
            console.error("Failed to delete trusted device");
        });
    }
});

export default new TrustedDevices();
