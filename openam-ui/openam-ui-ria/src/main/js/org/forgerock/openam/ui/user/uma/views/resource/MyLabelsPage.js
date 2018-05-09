/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "backbone",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/user/uma/views/resource/BasePage",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/user/uma/services/UMAService",
    "templates/user/uma/views/resource/_DeleteLabelButton"
], ($, Backbone, Backgrid, BackgridUtils, BasePage, BootstrapDialog, Configuration, Constants, EventManager,
    Router, UMAService, DeleteLabelButtonPartial) => {
    var MyLabelsPage = BasePage.extend({
        template: "user/uma/views/resource/MyLabelsPageTemplate",
        partials: {
            "templates/user/uma/views/resource/_DeleteLabelButton": DeleteLabelButtonPartial
        },
        events: {
            "click button#deleteLabel": "deleteLabel"
        },
        deleteLabel () {
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

                        UMAService.labels.remove(self.data.label._id).done(() => {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteLabelSuccess");

                            dialog.close();
                            Router.routeTo(Router.configuration.routes.umaResourcesMyResources, {
                                trigger: true,
                                args: []
                            });
                        }).fail(() => {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteLabelFail");

                            dialog.enableButtons(true);
                            dialog.getButton("ok").text($.t("common.form.ok"));
                        });
                    }
                }];

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.resources.myLabels.deleteLabel.dialog.title"),
                message: $.t("uma.resources.myLabels.deleteLabel.dialog.message"),
                closable: false,
                buttons
            });
        },
        recordsPresent () {
            this.$el.find("button#deleteLabel").prop("disabled", false);
        },
        render (args, callback) {
            var labelId = args[0],
                self = this;

            UMAService.labels.get(labelId).then((result) => {
                self.data.label = result;
                if (result) {
                    self.renderGrid(self.createLabelCollection(labelId), self.createColumns(`mylabels/${labelId}`),
                        callback);
                } else {
                    self.parentRender(callback);
                }
            });
        }
    });

    return MyLabelsPage;
});
