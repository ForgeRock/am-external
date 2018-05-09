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
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView",
    "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesTemplate",
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesToolbarTemplate",
    "partials/util/_HelpLink",
    "templates/admin/backgrid/cell/IconAndNameCell",
    "templates/admin/backgrid/cell/RowActionsCell"
], ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, Messages, EventManager, Router,
    Constants, UIUtils, URLHelper, AbstractListView, ResourceTypeModel, BackgridUtils,
    ResourceTypesTemplate, ResourceTypesToolbarTemplate, HelpLinkPartial, IconAndNameCellTemplate,
    RowActionsCellTemplate) =>
    AbstractListView.extend({
        template: ResourceTypesTemplate,
        // Used in AbstractListView
        toolbarTemplate: ResourceTypesToolbarTemplate,
        partials: {
            "util/_HelpLink": HelpLinkPartial
        },
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
            this.data.headerActions = [{ actionPartial: "util/_HelpLink", helpLink: "backstage.authz.resourceTypes" }];
            this.data.items.fetch({ reset: true }).done((response) => {
                if (response.resultCount > 0) {
                    this.data.hasResourceTypes = true;
                    this.renderTable(callback);
                } else {
                    this.data.hasResourceTypes = false;
                    this.parentRender(this.renderToolbar);
                }
            }).fail(() => {
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
    })
);
