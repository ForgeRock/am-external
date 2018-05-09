/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone.paginator",
    "backbone",
    "backgrid-filter",
    "jquery",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/user/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator"
], (BackbonePaginator, Backbone, BackgridFilter, $, Backgrid, AbstractView, Configuration, Constants, fetchUrl,
    BackgridUtils, CommonShare) => {
    var BasePage = AbstractView.extend({
        createCollection (url, queryFilters) {
            var self = this;

            return Backbone.PageableCollection.extend({
                url: `${Constants.context}/json${url}`,
                queryParams: BackgridUtils.getQueryParams({
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryFilter: queryFilters,
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                }),
                state: BackgridUtils.getState(),
                parseState: BackgridUtils.parseState,
                parseRecords (data) {
                    if (data.result.length) {
                        self.recordsPresent();
                    }

                    return data.result;
                },
                sync: BackgridUtils.sync
            });
        },
        createLabelCollection (labelId) {
            var filters = [];

            if (labelId) {
                filters.push(`labels eq "${labelId}"`);
            }

            return this.createCollection(
                fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/oauth2/resources/sets`),
                filters.map(encodeURIComponent)
            );
        },
        createSetCollection (notResourceOwner) {
            var filters = [`resourceOwnerId eq "${Configuration.loggedUser.get("username")}"`];

            if (notResourceOwner) {
                filters[0] = `! ${filters[0]}`;
            }

            return this.createCollection(
                fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/oauth2/resources/sets`),
                filters.map(encodeURIComponent)
            );
        },
        createColumns (pathToResource) {
            return [{
                name: "name",
                label: $.t("uma.resources.grid.header.0"),
                cell: BackgridUtils.UriExtCell,
                headerCell: BackgridUtils.FilterHeaderCell,
                href (rawValue, formattedValue, model) {
                    return `#uma/resources/${pathToResource}/${model.get("_id")}`;
                },
                editable: false
            }, {
                name: "resourceServer",
                label: $.t("uma.resources.grid.header.1"),
                cell: "string",
                editable: false,
                headerCell: BackgridUtils.ClassHeaderCell
            }, {
                name: "type",
                label: $.t("uma.resources.grid.header.2"),
                cell: "string",
                headerCell: BackgridUtils.ClassHeaderCell,
                editable: false
            }, {
                name: "share",
                label: $.t("uma.resources.grid.header.3"),
                cell: Backgrid.Cell.extend({
                    className: "fr-col-btn-1 fa fa-share",
                    events: { "click": "share" },
                    share () {
                        var shareView = new CommonShare();
                        shareView.renderDialog({
                            _id: this.model.get("_id"),
                            toBeCreated: this.model.toBeCreated,
                            share: () => {
                                this.model.toBeCreated = false;
                            }
                        });
                    },
                    render () {
                        this.$el.attr({ "title": $.t("uma.share.shareResource") });
                        this.delegateEvents();
                        return this;
                    }
                }),
                editable: false,
                sortable: false,
                headerCell: BackgridUtils.ClassHeaderCell
            }];
        },
        recordsPresent () {
            // Override in child
        },
        renderGrid (Collection, columns, callback) {
            var self = this, grid, paginator;

            this.data.collection = new Collection();
            this.data.collection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns,
                className: "backgrid table",
                collection: this.data.collection,
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection: this.data.collection,
                windowSize: 3
            });

            this.parentRender(() => {
                self.$el.find(".table-container").append(grid.render().el);
                self.$el.find(".panel-body").append(paginator.render().el);

                self.data.collection.fetch({ reset: true, processData: false }).done(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        }
    });

    return BasePage;
});
