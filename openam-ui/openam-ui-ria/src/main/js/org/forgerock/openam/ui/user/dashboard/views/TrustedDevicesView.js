/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/dashboard/services/TrustedDevicesService"
], ($, _, AbstractView, TrustedDevicesService) => {
    var TrustedDevices = AbstractView.extend({
        template: "user/dashboard/TrustedDevicesTemplate",
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

    return new TrustedDevices();
});
