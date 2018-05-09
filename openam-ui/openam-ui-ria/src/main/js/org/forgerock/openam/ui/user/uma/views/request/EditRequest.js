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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/user/uma/services/UMAService",
    "org/forgerock/openam/ui/user/uma/views/backgrid/cells/PermissionsCell"
], ($, Backbone, Backgrid, AbstractView, Configuration, Router, Constants, fetchUrl, BackgridUtils,
    UMAService, PermissionsCell) => {
    var EditRequest = AbstractView.extend({
        template: "user/uma/views/request/EditRequestTemplate",
        events: {
            "click button[data-permission=allow]": "allowRequest",
            "click button[data-permission=deny]": "denyRequest"
        },

        allowRequest () {
            UMAService.approveRequest(this.model.get("_id"), this.model.get("permissions")).done(() => {
                Router.routeTo(Router.configuration.routes.umaRequestList, {
                    args: [],
                    trigger: true
                });
            });
        },

        denyRequest () {
            UMAService.denyRequest(this.model.get("_id")).done(() => {
                Router.routeTo(Router.configuration.routes.umaRequestList, {
                    args: [],
                    trigger: true
                });
            });
        },

        render (args, callback) {
            var self = this,
                id = null,
                columns,
                grid,
                RequestCollection;

            id = args[0];

            RequestCollection = Backbone.Collection.extend({
                url: `${Constants.context}/json${
                    fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/uma/pendingrequests/${id}`)
                }`
            });

            columns = [{
                name: "user",
                label: $.t("uma.requests.grid.header.0"),
                cell: "string",
                editable: false
            }, {
                name: "resource",
                label: $.t("uma.requests.grid.header.1"),
                cell: "string",
                editable: false
            }, {
                name: "when",
                label: $.t("uma.requests.grid.header.2"),
                cell: BackgridUtils.DatetimeAgoCell,
                editable: false
            }, {
                name: "permissions",
                label: $.t("uma.requests.grid.header.3"),
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-xs-7 col-md-6"
                }),
                cell: PermissionsCell.extend({
                    onChange (value) {
                        this.model.set("permissions", value, { silent: true });
                        var anySelected = value !== null;
                        this.$el.parent().find("[data-permission=allow]").prop("disabled", !anySelected);
                    }
                }),
                editable: false
            }];

            this.data.requests = new RequestCollection();

            grid = new Backgrid.Grid({
                columns,
                className: "backgrid table",
                collection: this.data.requests,
                emptyText: $.t("console.common.noResults")
            });

            this.parentRender(() => {
                self.$el.find(".table-container").append(grid.render().el);
                self.data.requests.fetch({ reset: true, processData: false }).done(() => {
                    self.model = self.data.requests.findWhere({ _id: id });
                    if (callback) { callback(); }
                }).fail(() => {
                    self.$el.find("button[data-permission]").prop("disabled", true);
                });
            });
        }
    });

    return new EditRequest();
});
