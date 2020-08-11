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
 * Copyright 2015-2019 ForgeRock AS.
 */

import "backbone.paginator";
import "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import Script from "org/forgerock/openam/ui/admin/models/scripts/ScriptModel";
import ScriptsService from "org/forgerock/openam/ui/admin/services/global/ScriptsService";
import ScriptsTemplate from "templates/admin/views/realms/scripts/ScriptsTemplate";
import ScriptsToolbarTemplate from "templates/admin/views/realms/scripts/ScriptsToolbarTemplate";
import ThemeableSelectAllCell from "org/forgerock/commons/ui/common/backgrid/extension/ThemeableSelectAllCell";
import URLHelper from "org/forgerock/openam/ui/common/util/URLHelper";

export default AbstractView.extend({
    template: ScriptsTemplate,
    toolbarTemplate: ScriptsToolbarTemplate,
    events: {
        "click [data-add-entity]": "addNewScript",
        "click [data-delete-scripts]": "onDeleteClick"
    },

    render (args, callback) {
        const self = this;

        this.realmPath = args[0];
        this.data.selectedUUIDs = [];
        this.contextSchemaPromise = ScriptsService.scripts.getSchema();
        this.languageSchemaPromise = ScriptsService.scripts.getContextSchema();

        const Scripts = Backbone.PageableCollection.extend({
            url: URLHelper.substitute("__api__/scripts"),
            model: Script,
            state: BackgridUtils.getState(),
            queryParams: BackgridUtils.getQueryParams(),
            parseState: BackgridUtils.parseState,
            parseRecords: BackgridUtils.parseRecords,
            sync: BackgridUtils.sync
        });

        const renderTranslatedCell = function () {
            const id = this.model.get(this.column.get("name"));
            const translation = (this.map && self[this.map]) ? self[this.map][id] : id;

            this.$el.text(translation);
            return this;
        };

        const columns = [{
            name: "",
            cell: ThemeableSelectAllCell,
            headerCell: "select-all"
        }, {
            name: "name",
            label: t("console.scripts.list.grid.0"),
            cell: "string",
            headerCell: BackgridUtils.FilterHeaderCell,
            sortType: "toggle",
            editable: false
        }, {
            name: "context",
            label: t("console.scripts.list.grid.1"),
            cell: Backgrid.StringCell.extend({
                map: "contextMap",
                render: renderTranslatedCell
            }),
            headerCell: BackgridUtils.FilterHeaderCell,
            sortType: "toggle",
            editable: false
        }, {
            name: "language",
            label: t("console.scripts.list.grid.2"),
            cell: Backgrid.StringCell.extend({
                map: "langMap",
                render: renderTranslatedCell
            }),
            headerCell: BackgridUtils.FilterHeaderCell,
            sortType: "toggle",
            editable: false
        }, {
            name: "description",
            label: t("console.scripts.list.grid.3"),
            cell: "string",
            sortable: false,
            editable: false
        }];

        const ClickableRow = BackgridUtils.ClickableRow.extend({
            attributes: () => ({ tabindex: 0 }),
            callback (e) {
                const $target = $(e.target);

                if ($target.is("input") || $target.is(".select-row-cell")) {
                    return;
                }

                Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                    args: [encodeURIComponent(self.realmPath), encodeURIComponent(this.model.id)],
                    trigger: true
                });
            }
        });

        this.data.scripts = new Scripts();

        this.data.scripts.on("backgrid:selected", (model, selected) => {
            self.onRowSelect(model, selected);
        });

        this.data.scripts.on("backgrid:sort", BackgridUtils.doubleSortFix);

        const grid = new Backgrid.Grid({
            columns,
            row: ClickableRow,
            collection: self.data.scripts,
            className: "backgrid table table-hover",
            emptyText: t("console.common.noResults")
        });

        const paginator = new Backgrid.Extension.ThemeablePaginator({
            collection: self.data.scripts,
            windowSize: 3
        });

        this.parentRender(function () {
            this.renderToolbar();

            this.$el.find(".table-container").append(grid.render().el);
            this.$el.find(".panel-body").append(paginator.render().el);

            Promise.all([
                this.contextSchemaPromise,
                this.languageSchemaPromise
            ]).then(([contSchema, langSchema]) => {
                const languageSchema = langSchema ? langSchema.properties.languages.items : undefined;
                const contextSchema = contSchema ? contSchema.properties.defaultContext : undefined;
                self.langMap = self.createMapBySchema(languageSchema);
                self.contextMap = self.createMapBySchema(contextSchema);

                self.data.scripts.fetch({ reset: true }).then(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        });
    },

    onDeleteClick (e) {
        const msg = { message: t("console.scripts.list.confirmDeleteText") };
        e.preventDefault();
        FormHelper.showConfirmationBeforeDeleting(msg, _.bind(this.deleteRecords, this));
    },

    deleteRecords () {
        const self = this;
        let i = 0;
        let item;
        const onDestroy = function () {
            self.data.selectedUUIDs = [];
            self.data.scripts.fetch({ reset: true });
            self.renderToolbar();
        };
        const onSuccess = function () {
            onDestroy();
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        };

        for (; i < this.data.selectedUUIDs.length; i++) {
            item = this.data.scripts.get(this.data.selectedUUIDs[i]);

            item.destroy({
                success: onSuccess,
                wait: true
            });
        }
    },

    onRowSelect (model, selected) {
        if (selected) {
            if (!_.includes(this.data.selectedUUIDs, model.id)) {
                this.data.selectedUUIDs.push(model.id);
            }
        } else {
            this.data.selectedUUIDs = _.without(this.data.selectedUUIDs, model.id);
        }

        this.renderToolbar();
    },

    renderToolbar () {
        const self = this;
        const tpl = self.toolbarTemplate(self.data);
        self.$el.find("[data-grid-toolbar]").html(tpl);
    },

    addNewScript () {
        Router.routeTo(Router.configuration.routes.realmsScriptNew, {
            args: [encodeURIComponent(this.realmPath)],
            trigger: true
        });
    },

    // TODO: server side fix is needed instead of this function
    createMapBySchema (schema) {
        let map; let i; let length;

        if (schema && schema["enum"]) {
            map = {};
            length = schema["enum"].length;

            for (i = 0; i < length; i++) {
                map[schema["enum"][i]] = schema.options.enum_titles[i];
            }
        }
        return map;
    }
});
