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

import $ from "jquery";

import BasePage from "org/forgerock/openam/ui/user/uma/views/resource/BasePage";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import DeleteLabelButtonPartial from "templates/user/uma/views/resource/_DeleteLabelButton";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Router from "org/forgerock/commons/ui/common/main/Router";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";

const MyLabelsPage = BasePage.extend({
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

                    UMAService.labels.remove(self.data.label._id).then(() => {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteLabelSuccess");

                        dialog.close();
                        Router.routeTo(Router.configuration.routes.umaResourcesMyResources, {
                            trigger: true,
                            args: []
                        });
                    }, () => {
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

export default MyLabelsPage;
