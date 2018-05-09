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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "templates/common/DefaultBaseTemplate"
], ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, AbstractView,
    Configuration, Constants, fetchUrl, BackgridUtils, DefaultBaseTemplate) => {
    var HistoryView = AbstractView.extend({
        template: "user/uma/views/history/ListHistory",
        baseTemplate: DefaultBaseTemplate,
        events: {},

        render () {
            var self = this,
                collection,
                grid,
                paginator;

            collection = new (Backbone.PageableCollection.extend({
                url: `${Constants.context}/json${
                    fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/uma/auditHistory`)}`,
                state: {
                    pageSize: 10,
                    sortKey: "eventTime",
                    order: 1
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryFilter: BackgridUtils.queryFilter,
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                },
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            }))();

            grid = new Backgrid.Grid({
                columns: [{
                    name: "requestingPartyName",
                    label: $.t("uma.history.grid.header.0"),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    cell: "string",
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "resourceSetName",
                    label: $.t("uma.history.grid.header.1"),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    cell: BackgridUtils.UriExtCell,
                    href (rawValue, formattedValue, model) {
                        return `#uma/resources/myresources/all/${encodeURIComponent(model.get("resourceSetId"))}`;
                    },
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "type",
                    label: $.t("uma.history.grid.header.2"),
                    cell: "string",
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw (rawValue) {
                            return $.t(`uma.history.grid.types.${rawValue.toLowerCase()}`);
                        }
                    }),
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "eventTime",
                    label: $.t("uma.history.grid.header.3"),
                    cell: BackgridUtils.DatetimeAgoCell,
                    editable: false,
                    sortType: "toggle"
                }],
                emptyText: $.t("console.common.noResults"),
                className:"backgrid table",
                collection
            });

            collection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection,
                windowSize: 3
            });

            self.parentRender(() => {
                self.$el.find(".table-container").append(grid.render().el);
                self.$el.find(".panel-body").append(paginator.render().el);
                collection.fetch({ processData: false, reset: true });
            });
        }
    });

    return new HistoryView();
});
