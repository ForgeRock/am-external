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

import "backbone.paginator";
import "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator";

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import AbstractListView from "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import IconAndNameCellTemplate from "templates/admin/backgrid/cell/IconAndNameCell";
import ResourceTypeModel from "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel";
import ResourceTypesTemplate from "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesTemplate";
import ResourceTypesToolbarTemplate from
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesToolbarTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import RowActionsCellTemplate from "templates/admin/backgrid/cell/RowActionsCell";
import URLHelper from "org/forgerock/openam/ui/common/util/URLHelper";

export default AbstractListView.extend({
    template: ResourceTypesTemplate,
    // Used in AbstractListView
    toolbarTemplate: ResourceTypesToolbarTemplate,
    events: {
        "click [data-add-entity]": "addNewResourceType"
    },
    render (args, callback) {
        const ResourceTypes = Backbone.PageableCollection.extend({
            url: URLHelper.substitute("__api__/resourcetypes"),
            model: ResourceTypeModel,
            state: BackgridUtils.getState(),
            queryParams: BackgridUtils.getQueryParams({
                _queryFilter: [
                    `name+eq+${encodeURIComponent('"^(?!Delegation Service$).*"')}`
                ]
            }),
            parseState: BackgridUtils.parseState,
            parseRecords: BackgridUtils.parseRecords,
            sync (method, model, options) {
                options.beforeSend = function (xhr) {
                    xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
                };
                return BackgridUtils.sync(method, model, options);
            }
        });

        this.realmPath = args[0];
        this.data.items = new ResourceTypes();
        this.data.items.fetch({ reset: true }).then((response) => {
            if (response.resultCount > 0) {
                this.data.hasResourceTypes = true;
                this.renderTable(callback);
            } else {
                this.data.hasResourceTypes = false;
                this.parentRender(this.renderToolbar);
            }
        }, () => {
            Router.routeTo(Router.configuration.routes.realms, { args: [], trigger: true });
        });
    },

    renderTable (callback) {
        const self = this;
        const ClickableRow = BackgridUtils.ClickableRow.extend({
            callback (e) {
                var $target = $(e.target);

                if ($target.parents().hasClass("fr-col-btn-2")) {
                    return;
                }

                Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                    args: _.map([self.realmPath, this.model.id], encodeURIComponent),
                    trigger: true
                });
            }
        });

        const columns = [
            {
                name: "name",
                label: $.t("console.authorization.resourceTypes.list.grid.0"),
                cell: BackgridUtils.TemplateCell.extend({
                    iconClass: "fa-cube",
                    template: IconAndNameCellTemplate,
                    rendered () {
                        this.$el.find("i.fa").addClass(this.iconClass);
                    }
                }),
                headerCell: BackgridUtils.FilterHeaderCell,
                sortType: "toggle",
                editable: false
            },
            {
                name: "",
                cell: BackgridUtils.TemplateCell.extend({
                    className: "fr-col-btn-2",
                    template: RowActionsCellTemplate,
                    events: {
                        "click [data-edit-item]": "editItem",
                        "click [data-delete-item]": "deleteItem"
                    },
                    editItem (e) {
                        self.editRecord(e, this.model.id, Router.configuration.routes.realmsResourceTypeEdit);
                    },
                    deleteItem (e) {
                        self.onDeleteClick(e, { type: $.t("console.authorization.common.resourceType") },
                            this.model.id);
                    }
                }),
                sortable: false,
                editable: false
            }
        ];

        const grid = new Backgrid.Grid({
            columns,
            row: ClickableRow,
            collection: this.data.items,
            className: "backgrid table table-hover",
            emptyText: $.t("console.common.noResults")
        });

        const paginator = new Backgrid.Extension.ThemeablePaginator({
            collection: this.data.items,
            windowSize: 3
        });

        this.bindDefaultHandlers();
        this.parentRender(() => {
            this.renderToolbar();
            this.$el.find(".table-container").append(grid.render().el);
            this.$el.find(".panel-body").append(paginator.render().el);

            if (callback) { callback(); }
        });
    },

    addNewResourceType () {
        Router.routeTo(Router.configuration.routes.realmsResourceTypeNew, {
            args: [encodeURIComponent(this.realmPath)],
            trigger: true
        });
    }
});
