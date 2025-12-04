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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

import "backbone.paginator";
import "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator";

import $ from "jquery";
import Backbone from "backbone";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ActionsCellTemplate from "themes/default/templates/user/uma/backgrid/cell/ActionsCell";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import IconWithTooltipMessageCellTemplate from
    "themes/default/templates/user/uma/backgrid/cell/IconWithTooltipMessageCell";
import PermissionsCell from "org/forgerock/openam/ui/user/uma/views/backgrid/cells/PermissionsCell";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";

const ListRequest = AbstractView.extend({
    template: "user/uma/views/request/ListRequestTemplate",

    render (args, callback) {
        const self = this;
        const RequestsCollection = Backbone.PageableCollection.extend({
            url: `${Constants.context}/json${
                fetchUrl(`/users/${Configuration.loggedUser.get("username")}/uma/pendingrequests`)}`,
            state: {
                pageSize: 10,
                sortKey: "user"
            },
            queryParams: {
                pageSize: "_pageSize",
                _sortKeys: BackgridUtils.sortKeys,
                _queryId: "*",
                _queryFilter: "true",
                _pagedResultsOffset: BackgridUtils.pagedResultsOffset
            },
            parseState: BackgridUtils.parseState,
            parseRecords: BackgridUtils.parseRecords,
            sync: BackgridUtils.sync
        });

        const columns = [{
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

                    const anySelected = value !== null;
                    this.$el.parent().find("[data-permission=allow]").prop("disabled", !anySelected);

                    // TODO: Code that works with Backbone MultiSelect
                    // var anySelected = this.$el.find("li.active").length > 0;
                    // this.$el.parent().find("[data-permission=allow]").prop("disabled", !anySelected);
                }
            }),
            editable: false
        }, {
            name: "userContainsConfusables",
            label: "",
            cell: BackgridUtils.TemplateCell.extend({
                iconClass: "fa-warning",
                template: IconWithTooltipMessageCellTemplate,
                render () {
                    this.$el.html(this.template());
                    if (this.model.get("userContainsConfusables") === true) {
                        this.$el.find("i.fa").addClass(this.iconClass);
                        this.$el.find('[data-toggle="tooltip"]').removeAttr("aria-hidden");
                        this.$el.find('[data-toggle="tooltip"]').tooltip();
                    }
                    return this;
                }
            }),
            editable: false
        }, {
            name: "actions",
            label: "",
            cell: BackgridUtils.TemplateCell.extend({
                template: ActionsCellTemplate,
                events: {
                    "click button[data-permission=allow]": "allow",
                    "click button[data-permission=deny]": "deny"
                },
                allow () {
                    UMAService.approveRequest(this.model.get("_id"), this.model.get("permissions")).then(() => {
                        self.data.requests.fetch({ reset: true, processData: false }); // TODO: DRY
                    });
                },
                deny () {
                    UMAService.denyRequest(this.model.get("_id")).then(() => {
                        self.data.requests.fetch({ reset: true, processData: false }); // TODO: DRY
                    });
                }
            }),
            editable: false
        }];

        this.data.requests = new RequestsCollection();
        this.data.requests.on("backgrid:sort", BackgridUtils.doubleSortFix);

        const grid = new Backgrid.Grid({
            columns,
            className: "backgrid table",
            collection: self.data.requests,
            emptyText: $.t("console.common.noResults")
        });

        const paginator = new Backgrid.Extension.ThemeablePaginator({
            collection: self.data.requests,
            windowSize: 3
        });

        self.data.requests.fetch({ reset: true, processData: false }).then(() => {
            if (callback) { callback(); }
        });

        this.parentRender(() => {
            self.$el.find(".table-container").append(grid.render().el);
            self.$el.find(".panel-body").append(paginator.render().el);
        });
    }
});

export default new ListRequest();
