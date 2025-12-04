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

import "selectize";

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import CommonShare from "org/forgerock/openam/ui/user/uma/views/share/CommonShare";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import LabelTreeNavigationView from "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import RevokeCellTemplate from "themes/default/templates/user/uma/backgrid/cell/RevokeCell";
import SelectizeCell from "themes/default/templates/user/uma/backgrid/cell/SelectizeCell";
import UMAResourceSetWithPolicy from "org/forgerock/openam/ui/user/uma/models/UMAResourceSetWithPolicy";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";
import IconWithTooltipMessageCellTemplate from
    "themes/default/templates/user/uma/backgrid/cell/IconWithTooltipMessageCell";

function isUserLabel (label) {
    return label.type === "USER";
}
function getLabelForId (labels, labelId) {
    return _.find(labels, (otherLabel) => {
        return otherLabel._id === labelId;
    });
}
function getLabelForName (labels, name) {
    return _.find(labels, (otherLabel) => {
        return otherLabel.name === name;
    });
}
function createLabels (labelNames) {
    const creationPromises = _.map(labelNames, (labelName) => {
        return UMAService.labels.create(labelName, "USER");
    });
    return Promise.all(creationPromises).then(function () {
        if (creationPromises.length === 1) {
            return [arguments[0]._id];
        } else {
            return _.map(arguments, (arg) => {
                return arg[0]._id;
            });
        }
    });
}
function getAllLabels () {
    return UMAService.labels.all().then((labels) => {
        return labels.result;
    });
}
const ResourcePage = AbstractView.extend({
    initialize () {
        // TODO: AbstarctView.prototype.initialize.call(this);
        this.model = null;
    },
    template: "user/uma/views/resource/ResourceTemplate",
    selectizeTemplate: SelectizeCell,
    events: {
        "click button#starred": "onToggleStarred",
        "click button#share": "onShare",
        "click li#unshare": "onUnshare",
        "click button#editLabels": "editLabels",
        "click .page-toolbar .selectize-input.disabled": "editLabels",
        "click button#saveLabels": "submitLabelsChanges",
        "click button#discardLabels": "discardLabelsChanges"
    },
    onModelError (model, response) {
        console.error(`Unrecoverable load failure UMAResourceSetWithPolicy. ${response.responseJSON.code} (${
            response.responseJSON.reason
        })${response.responseJSON.message}`);
    },
    onModelChange (model) {
        this.render([undefined, model.get("_id")]);
    },
    onUnshare (event) {
        if ($(event.currentTarget).hasClass("disabled")) { return false; }
        event.preventDefault();

        const self = this;

        BootstrapDialog.show({
            type: BootstrapDialog.TYPE_DANGER,
            title: self.model.get("name"),
            message: $.t("uma.resources.show.revokeAllMessage"),
            closable: false,
            buttons: [{
                label: $.t("common.form.cancel"),
                action (dialog) {
                    dialog.close();
                }
            }, {
                id: "btnOk",
                label: $.t("common.form.ok"),
                cssClass: "btn-primary btn-danger",
                action (dialog) {
                    dialog.enableButtons(false);
                    dialog.getButton("btnOk").text($.t("common.form.working"));
                    self.model.get("policy").destroy().then(() => {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesSuccess");
                        self.onModelChange(self.model);
                        self.model.toBeCreated = true;
                    }, (response) => {
                        Messages.addMessage({
                            response,
                            type: Messages.TYPE_DANGER
                        });
                    }).always(() => {
                        dialog.close();
                    });
                }
            }]
        });
    },
    onShare () {
        const shareView = new CommonShare();
        shareView.renderDialog({
            _id: this.model.id,
            toBeCreated: this.model.toBeCreated,
            share: () => {
                this.model.toBeCreated = false;
                this.updateUnshareButton();
            }
        });
    },
    onToggleStarred () {
        const self = this;

        const starredLabelId = _.find(this.allLabels, { type: "STAR" })._id;

        const starButton = self.$el.find("#starred");

        const starIcon = starButton.find("i");

        self.model.toggleStarred(starredLabelId);
        starIcon.removeClass("fa-star-o fa-star");
        starIcon.addClass("fa-refresh fa-spin");
        starButton.attr("disabled", true);

        self.model.save().always(() => {
            const isStarred = _.includes(self.model.get("labels"), starredLabelId);
            starButton.attr("disabled", false);
            starIcon.removeClass("fa-refresh fa-spin");
            starIcon.addClass(isStarred ? "fa-star" : "fa-star-o");
        });
    },
    renderLabelsOptions () {
        const self = this;

        const labelsSelect = this.$el.find("#labels select").selectize({
            plugins: ["restore_on_backspace"],
            delimiter: false,
            persist: false,
            create: true,
            hideSelected: true,
            onChange () {
                self.$el.find("button#saveLabels").prop("disabled", false);
            },
            render: {
                item (item) {
                    return `<div data-value="${item.name}" class="item">${item.name}</div>"`;
                }
            },
            labelField: "name",
            valueField: "name",
            searchField: ["name"]
        })[0];
        labelsSelect.selectize.disable();
        this.updateLabelOptions();
        this.$el.find(".page-toolbar .btn-group").hide();
    },
    updateLabelOptions () {
        const labelsSelectize = this.getLabelSelectize();

        const userLabels = _.filter(this.allLabels, isUserLabel);

        const resourceUserLabelNames = _(this.model.get("labels"))
            .map(_.partial(getLabelForId, this.allLabels))
            .filter(isUserLabel)
            .sortBy("name")
            .map("name")
            .value();
        labelsSelectize.clearOptions();
        labelsSelectize.addOption(userLabels);
        labelsSelectize.clear();
        _.each(resourceUserLabelNames, (item) => {
            labelsSelectize.addItem(item);
        });
    },
    renderSelectizeCell () {
        const promise = $.Deferred();

        const template = this.selectizeTemplate({});
        const SelectizeCell = Backgrid.Cell.extend({
            className: "selectize-cell",
            render () {
                this.$el.html(template);
                const select = this.$el.find("select").selectize({
                    create: false,
                    delimiter: false,
                    dropdownParent: "#uma",
                    hideSelected: true,
                    persist: false,
                    labelField: "name",
                    valueField: "id",
                    items: this.model.get("scopes").pluck("name"),
                    options: this.model.get("scopes").toJSON()
                })[0];
                select.selectize.disable();

                /* This an extention of the original positionDropdown method within Selectize. The override is
                    * required because using the dropdownParent 'body' places the dropdown out of scope of the
                    * containing backbone view. However adding the dropdownParent as any other element,
                    * has problems due the offsets and/positioning being incorrecly calucaluted in orignal
                    * positionDropdown method.
                    */
                select.selectize.positionDropdown = function () {
                    const $control = this.$control;

                    const offset = this.settings.dropdownParent ? $control.offset() : $control.position();

                    if (this.settings.dropdownParent) {
                        offset.top -= $control.outerHeight(true) * 2 +
                                        $(this.settings.dropdownParent).position().top;
                        offset.left -= $(this.settings.dropdownParent).offset().left +
                                        $(this.settings.dropdownParent).outerWidth() -
                                        $(this.settings.dropdownParent).outerWidth(true);
                    } else {
                        offset.top += $control.outerHeight(true);
                    }

                    this.$dropdown.css({
                        width: $control.outerWidth(),
                        top: offset.top,
                        left: offset.left
                    });
                };

                this.delegateEvents();
                return this;
            }
        });
        promise.resolve(SelectizeCell);

        return promise;
    },

    render (args, callback) {
        const id = _.last(args); const self = this;

        if (this.model && this.id === id) {
            if (!this.isCurrentlyFetchingData) {
                this.renderWithModel(callback);
            }
        } else {
            this.isCurrentlyFetchingData = true;
            Promise.all([
                getAllLabels(),
                UMAResourceSetWithPolicy.findOrCreate({ _id: id }).fetch()
            ]).then(([allLabels, model]) => {
                // Ensure we don't render any previous requests that were cancelled.
                if (model.id === self.id) {
                    self.allLabels = allLabels;
                    self.model = model;
                    self.isCurrentlyFetchingData = false;
                    self.renderWithModel(callback);
                }
            }, this.onModelError);
        }
        this.id = id;
    },
    renderWithModel (callback) {
        let collection; let grid; const self = this;

        /**
         * FIXME: Ideally the data needs to the be whole model, but I'm told it's also global so we're
         * copying in just the attributes I need ATM
         */
        this.data = {};
        this.data.name = this.model.get("name");
        this.data.icon = this.model.get("icon_uri");

        // FIXME: Re-enable filtering and pagination
        //     UserPoliciesCollection = Backbone.PageableCollection.extend({
        //         url: URLHelper.substitute("__api__/users/__username__/oauth2/resourcesets/" + args[0]),
        //         parseRecords: function (data, options) {
        //             return data.policy.permissions;
        //         },
        //         sync: backgridUtils.sync
        //     });

        const RevokeCell = BackgridUtils.TemplateCell.extend({
            template: RevokeCellTemplate,
            events: {
                "click #revoke": "revoke"
            },
            revoke () {
                self.model.get("policy").get("permissions").remove(this.model);
                self.model.get("policy").save().then(() => {
                    self.onModelChange(self.model);
                });
            },
            className: "fr-col-btn-1"
        });

        /**
         * There *might* be no policy object present (if all the permissions were removed) so we're
         * checking for this and creating an empty collection if there is no policy
         */
        collection = new Backbone.Collection();
        if (this.model.has("policy")) {
            collection = this.model.get("policy").get("permissions");
        }

        this.renderSelectizeCell().then((SelectizeCell) => {
            grid = new Backgrid.Grid({
                columns: [{
                    name: "subject",
                    label: $.t("uma.resources.show.grid.0"),
                    cell: "string",
                    editable: false
                }, {
                    name: "permissions",
                    label: $.t("uma.resources.show.grid.2"),
                    cell: SelectizeCell,
                    editable: false,
                    sortable: false
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
                    name: "edit",
                    label: "",
                    cell: RevokeCell,
                    editable: false,
                    sortable: false,
                    headerCell: BackgridUtils.ClassHeaderCell.extend({
                        className: "fr-col-btn-1"
                    })
                }],
                collection,
                emptyText: $.t("console.common.noResults"),
                className: "backgrid table"
            });

            // FIXME: Re-enable filtering and pagination
            // paginator = new Backgrid.Extension.Paginator({
            //     collection: this.model.get('policy').get('permissions'),
            //     windowSize: 3
            // });

            self.parentRender(function () {
                self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
                self.renderLabelsOptions();

                self.updateUnshareButton();

                self.$el.find(".table-container").append(grid.render().el);
                // FIXME: Re-enable filtering and pagination
                // self.$el.find("#paginationContainer").append(paginator.render().el);

                self.$el.find("#umaShareImage img").error(function () {
                    $(this).parent().addClass("fa-file-image-o no-image");
                });

                const starredLabel = _.find(this.allLabels, { type: "STAR" });

                const isStarred = _.includes(this.model.get("labels"), starredLabel._id);

                if (isStarred) {
                    self.$el.find("#starred i").toggleClass("fa-star-o fa-star");
                }

                if (callback) { callback(); }
            });
        });
    },
    getLabelSelectize () {
        return this.$el.find("#labels select")[0].selectize;
    },
    stopEditingLabels () {
        const labelsSelect = this.getLabelSelectize();
        labelsSelect.disable();
        this.$el.find(".page-toolbar .btn-group").hide();
        this.$el.find("#editLabels").show();
        this.$el.find("#labels .selectize-control").addClass("pull-left");
    },
    editLabels () {
        const labelsSelect = this.getLabelSelectize();
        labelsSelect.enable();
        labelsSelect.focus();
        this.$el.find("#editLabels").hide();
        this.$el.find("#labels .selectize-control").removeClass("pull-left");
        this.$el.find("button#saveLabels").prop("disabled", true);
        this.$el.find("button#discardLabels").prop("disabled", false);
        this.$el.find(".page-toolbar .btn-group").show();
    },
    disableLabelControls () {
        const labelsSelect = this.getLabelSelectize();
        labelsSelect.disable();
        this.$el.find("button#saveLabels").prop("disabled", true);
        this.$el.find("button#discardLabels").prop("disabled", true);
    },
    enableLabelControls () {
        const labelsSelect = this.getLabelSelectize();
        labelsSelect.enable();
        this.$el.find("button#saveLabels").prop("disabled", false);
        this.$el.find("button#discardLabels").prop("disabled", false);
    },
    submitLabelsChanges () {
        const self = this;

        const labelsSelectize = this.getLabelSelectize();

        const selectedUserLabelNames = labelsSelectize.getValue();

        const userLabels = _.filter(this.allLabels, isUserLabel);

        const userLabelNames = _.map(userLabels, "name");

        const newUserLabelNames = _.difference(selectedUserLabelNames, userLabelNames);

        const existingUserLabelNames = _.intersection(selectedUserLabelNames, userLabelNames);

        const existingUserLabelIds = _(existingUserLabelNames)
            .map(_.partial(getLabelForName, userLabels))
            .map("_id")
            .value();

        const existingNonUserLabelIds = _(self.model.get("labels"))
            .map(_.partial(getLabelForId, this.allLabels))
            .reject(isUserLabel)
            .map("_id")
            .value();

        const existingLabelIds = existingUserLabelIds.concat(existingNonUserLabelIds);

        self.disableLabelControls();
        createLabels(newUserLabelNames).then((newIds) => {
            self.model.set("labels", existingLabelIds.concat(newIds));
            return Promise.all([getAllLabels(), self.model.save()]);
        }).then(([allLabels]) => {
            self.allLabels = allLabels;
            self.enableLabelControls();
            self.stopEditingLabels();
            self.updateLabelOptions();
            LabelTreeNavigationView.addUserLabels(_.filter(self.allLabels, isUserLabel));
        }, () => {
            self.enableLabelControls();
        });
    },
    discardLabelsChanges () {
        this.stopEditingLabels();
        this.updateLabelOptions();
    },
    updateUnshareButton () {
        if (this.model.has("policy") && this.model.get("policy").get("permissions").length > 0) {
            this.$el.find("li#unshare").removeClass("disabled").find("a").attr("aria-disabled", false);
        }
    }
});
export default ResourcePage;
