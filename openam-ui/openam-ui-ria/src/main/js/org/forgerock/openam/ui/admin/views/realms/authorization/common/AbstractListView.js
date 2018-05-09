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
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/BackgridUtils"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, FormHelper, BackgridUtils) => {
    return AbstractView.extend({
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
});
