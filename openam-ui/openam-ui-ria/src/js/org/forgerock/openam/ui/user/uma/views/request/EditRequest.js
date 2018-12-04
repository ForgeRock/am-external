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

import Backbone from "backbone";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Router from "org/forgerock/commons/ui/common/main/Router";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";
import PermissionsCell from "org/forgerock/openam/ui/user/uma/views/backgrid/cells/PermissionsCell";
var EditRequest = AbstractView.extend({
    template: "user/uma/views/request/EditRequestTemplate",
    events: {
        "click button[data-permission=allow]": "allowRequest",
        "click button[data-permission=deny]": "denyRequest"
    },

    allowRequest () {
        UMAService.approveRequest(this.model.get("_id"), this.model.get("permissions")).then(() => {
            Router.routeTo(Router.configuration.routes.umaRequestList, {
                args: [],
                trigger: true
            });
        });
    },

    denyRequest () {
        UMAService.denyRequest(this.model.get("_id")).then(() => {
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
                fetchUrl(`/users/${Configuration.loggedUser.get("username")}/uma/pendingrequests/${id}`)
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
            self.data.requests.fetch({ reset: true, processData: false }).then(() => {
                self.model = self.data.requests.findWhere({ _id: id });
                if (callback) { callback(); }
            }, () => {
                self.$el.find("button[data-permission]").prop("disabled", true);
            });
        });
    }
});

export default new EditRequest();
