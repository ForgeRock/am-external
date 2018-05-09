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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "templates/admin/views/realms/authentication/SettingsTemplate",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, AbstractView, Constants, EventManager, Form, Messages, SMSServiceUtils, AuthenticationService,
    SettingsTemplate) => {
    var SettingsView = AbstractView.extend({
        template: SettingsTemplate,
        events: {
            "click [data-revert]"          : "revert",
            "click [data-save]"            : "save",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render (args, callback) {
            var self = this;

            this.data.realmLocation = args[0];

            AuthenticationService.authentication.get(this.data.realmLocation).then((data) => {
                self.data.formData = data;

                self.parentRender(() => {
                    self.$el.find("div.tab-pane").show(); // FIXME: To remove
                    self.$el.find("ul.nav a:first").tab("show");

                    self.$el.find(".tab-menu .nav-tabs").tabdrop();

                    if (callback) {
                        callback();
                    }
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        renderTab (event) {
            this.$el.find("#tabpanel").empty();

            var id = $(event.target).attr("href").slice(1),
                schema = SMSServiceUtils.sanitizeSchema(this.data.formData.schema.properties[id]),
                element = this.$el.find("#tabpanel").get(0);

            this.data.form = new Form(element, schema, this.data.formData.values[id]);
            this.$el.find("[data-header]").parent().hide();
        },
        revert () {
            this.data.form.reset();
        },
        save () {
            var formData = this.data.form.data(),
                self = this;

            AuthenticationService.authentication.update(this.data.realmLocation, formData).then((data) => {
                // update formData for correct re-render tab after saving
                _.extend(self.data.formData.values, data);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        }
    });

    return SettingsView;
});
