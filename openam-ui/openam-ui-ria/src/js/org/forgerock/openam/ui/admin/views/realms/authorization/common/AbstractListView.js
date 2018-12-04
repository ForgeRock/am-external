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

import _ from "lodash";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Router from "org/forgerock/commons/ui/common/main/Router";

export default AbstractView.extend({
    toolbarTemplateID: "[data-grid-toolbar]",

    initialize () {
        AbstractView.prototype.initialize.call(this);
    },

    onDeleteClick (e, msg, id, callback) {
        e.preventDefault();

        FormHelper.showConfirmationBeforeDeleting(msg, _.bind(this.deleteRecord, this, id, callback));
    },

    deleteRecord (id, callback) {
        var self = this,
            item = self.data.items.get(id),
            onSuccess = function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

                if (callback) {
                    callback();
                }
            };

        item.destroy({
            success: onSuccess,
            wait: true
        }).always(() => {
            self.data.items.fetch({ reset: true });
        });
    },

    editRecord (e, id, route) {
        var self = this;

        Router.routeTo(route, {
            args: _.map([self.realmPath, id], encodeURIComponent),
            trigger: true
        });
    },

    bindDefaultHandlers () {
        this.data.items.on("backgrid:sort", BackgridUtils.doubleSortFix);
    },

    renderToolbar () {
        var self = this;

        const tpl = this.toolbarTemplate(this.data);
        self.$el.find(self.toolbarTemplateID).html(tpl);
    }
});
