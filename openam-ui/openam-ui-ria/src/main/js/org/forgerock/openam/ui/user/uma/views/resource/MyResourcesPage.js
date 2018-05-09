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
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openam/ui/user/uma/views/resource/BasePage",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/user/uma/services/UMAService",
    "templates/user/uma/views/resource/_UnshareAllResourcesButton"
], ($, _, BootstrapDialog, BasePage, Configuration, Constants, EventManager, UMAService,
    UnshareAllResourcesButtonPartial) => {
    var MyResourcesPage = BasePage.extend({
        template: "user/uma/views/resource/MyResourcesPageTemplate",
        partials: {
            "templates/user/uma/views/resource/_UnshareAllResourcesButton": UnshareAllResourcesButtonPartial
        },
        events: {
            "click button#unshareAllResources": "unshareAllResources"
        },
        recordsPresent () {
            this.$el.find("button#unshareAllResources").prop("disabled", false);
        },
        render (args, callback) {
            this.data.labelId = args[1];
            this.data.topLevel = args[1] === "";
            this.renderResources(callback);
        },
        renderResources (callback) {
            if (this.data.topLevel) {
                this.renderGrid(this.createSetCollection(), this.createColumns("myresources/all"), callback);
            } else {
                // Resolve label ID to name
                UMAService.labels.get(this.data.labelId).then((data) => {
                    const columns = this.createColumns(`myresources/${encodeURIComponent(data.id)}`);
                    // Splice out the "Hosts" column
                    columns.splice(1, 1);

                    this.data.labelName = data.name;
                    this.renderGrid(this.createLabelCollection(this.data.labelId), columns, callback);
                });
            }
        },
        unshareAllResources () {
            var self = this,
                buttons = [{
                    label: $.t("common.form.cancel"),
                    action (dialog) {
                        dialog.close();
                    }
                }, {
                    id: "ok",
                    label: $.t("common.form.ok"),
                    cssClass: "btn-primary btn-danger",
                    action (dialog) {
                        dialog.enableButtons(false);
                        dialog.getButton("ok").text($.t("common.form.working"));

                        UMAService.unshareAllResources().then(() => {
                            self.renderResources(() => {
                                _.forEach(self.data.collection.models, (model) => { model.toBeCreated = true; });
                                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
                                    "unshareAllResourcesSuccess");
                                dialog.close();
                            });
                        }, () => {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unshareAllResourcesFail");
                            dialog.enableButtons(true);
                            dialog.getButton("ok").text($.t("common.form.ok"));
                        });
                    }
                }];

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.resources.myresources.unshareAllResources.dialog.title"),
                message: $.t("uma.resources.myresources.unshareAllResources.dialog.message"),
                closable: false,
                buttons
            });
        }
    });

    return MyResourcesPage;
});