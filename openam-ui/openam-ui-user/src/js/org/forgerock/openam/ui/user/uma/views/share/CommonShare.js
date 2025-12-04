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

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import BackgridUtils from "org/forgerock/openam/ui/common/util/BackgridUtils";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";
import ShareCounter from "org/forgerock/openam/ui/user/uma/views/share/ShareCounter";
import UMAPolicyPermission from "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermission";
import UMAPolicyPermissionScope from "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermissionScope";
import UMAResourceSetWithPolicy from "org/forgerock/openam/ui/user/uma/models/UMAResourceSetWithPolicy";

const realmRegex = /[?&]realm=/;

const resourcesetsRegex = /\/json\/users\/[^/]+\/uma\/policies/;

const policyRegex = /\/json\/users\/[^/]+\/oauth2\/resourcesets/;

$.ajaxPrefilter((options) => {
    if ((resourcesetsRegex.test(options.url) || policyRegex.test(options.url)) && !realmRegex.test(options.url) &&
            RealmHelper.getOverrideRealm()) {
        options.url = RealmHelper.decorateURLWithOverrideRealm(options.url);
    }
});

const CommonShare = AbstractView.extend({
    initialize () {
        this.parentModel = null;
    },
    template: "user/uma/views/share/CommonShare",
    events: {
        "click input#shareButton": "save"
    },
    enableOrDisableShareButton () {
        const subjectValid = this.$el.find("#selectUser select")[0].selectize.getValue().length > 0;

        const permissionsValid = this.$el.find("#selectPermission select")[0].selectize.getValue().length > 0;

        this.$el.find("input#shareButton").prop("disabled", !(subjectValid && permissionsValid));
    },
    onParentModelError (model, response) {
        console.error(`Unrecoverable load failure UMAResourceSetWithPolicy. ${response.responseJSON.code} (${
            response.responseJSON.reason
        }) ${response.responseJSON.message}`);
        // TODO : Fire and event message
    },
    onParentModelSync (model) {
        // Hardwire the policyID into the policy as it's ID
        model.get("policy").set("policyId", this.parentModel.id);

        this.render();
    },

    /*
     * @returns Boolean whether the parent model required sync'ing
     */
    syncParentModel (id) {
        const syncRequired = !this.parentModel || (id && this.parentModel.id !== id);

        if (syncRequired) {
            this.stopListening(this.parentModel);

            this.parentModel = UMAResourceSetWithPolicy.findOrCreate({ _id: id });
            this.listenTo(this.parentModel, "sync", this.onParentModelSync);
            this.listenTo(this.parentModel, "error", this.onParentModelError);
            this.parentModel.fetch();
        }

        return syncRequired;
    },

    renderDialog (args, callback) {
        const self = this;

        const $div = $("<div></div>");

        const modelId = args._id || args;

        const options = {
            type: BootstrapDialog.TYPE_PRIMARY,
            title: $.t("uma.share.shareResource"),
            size: BootstrapDialog.SIZE_WIDE,
            cssClass: "shareDialog",
            message: $div,
            buttons: [{
                label: $.t("common.form.close"),
                cssClass: "btn-default",
                action (dialog) {
                    dialog.close();
                }
            }],
            onshow () {
                self.element = $div;
                self.render(modelId, callback);
            },

            onshown () {
                self.renderShareCounter(callback);
            }
        };

        this.toBeCreated = args.toBeCreated;
        this.afterShare = args.share;
        BootstrapDialog.show(options);
    },

    render (args, callback) {
        const self = this;

        // FIXME: Resolve unknown issue with args appearing as an Array
        if (args instanceof Array) {
            args = args[0];
        }
        /**
         * Guard clause to check if model requires sync'ing/updating
         * Reason: We do not know the id of the data we need until the render function is called with args,
         * thus we can only check at this point if we have the correct model to render this view (the model
         * might already contain the correct data).
         * Behaviour: If the model does require sync'ing then we abort this render via the return and render
         * will it invoked again when the model is updated
         */
        if (this.syncParentModel(args)) {
            return;
        }
        this.data = {};
        this.data.name = this.parentModel.get("name");
        this.data.icon = this.parentModel.get("icon_uri");
        this.data.scopes = this.parentModel.get("scopes").toJSON();
        const collection = this.parentModel.get("policy").get("permissions");
        this.data.permissions = collection.toJSON();

        const grid = new Backgrid.Grid({
            columns: [{
                name: "subject",
                label: $.t("uma.resources.show.grid.0"),
                cell: "string",
                editable: false,
                headerCell : BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-6"
                })
            },
            {
                name: "scopes",
                label: $.t("uma.resources.show.grid.2"),
                cell: Backgrid.Cell.extend({
                    render () {
                        const formatted = this.model.get("scopes").pluck("name").join(", ");
                        this.$el.empty();
                        this.$el.append(formatted);
                        this.delegateEvents();
                        return this;
                    }

                }),
                editable: false,
                headerCell : BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-6"
                })
            }],
            collection,
            emptyText: $.t("console.common.noResults"),
            className:"backgrid table"
        });

        this.parentRender(() => {
            self.renderUserOptions();
            self.renderPermissionOptions();
            self.renderShareCounter(callback);
            self.$el.find("#advancedView").append(grid.render().el);
            self.$el.find("#umaShareImage img").error(function () {
                $(this).parent().addClass("fa-file-image-o no-image");
            });
        });
    },
    renderPermissionOptions () {
        const self = this;

        this.$el.find("#selectPermission select").selectize({
            plugins: ["restore_on_backspace"],
            delimiter: false,
            persist: false,
            create: false,
            hideSelected: true,
            onChange () {
                self.enableOrDisableShareButton();
            }
        });
    },
    renderUserOptions () {
        const self = this;

        this.$el.find("#selectUser select").selectize({
            addPrecedence: true,
            create: true, // TODO: false when search for users is enabled
            // TODO: Disable looking up users
            // load: function (query, callback) {
            //     if (query.length < self.MIN_QUERY_LENGTH) {
            //         return callback();
            //     }
            //
            //     UMAService.searchUsers(query)
            //     .then(function (data) {
            //         return _.map(data.result, function (username) {
            //             return new User(username);
            //         });
            //     })
            //     .then(function (users) {
            //         callback(users);
            //     }, function (event) {
            //         console.error('error', event);
            //         callback();
            //     });
            // },
            onChange (value) {
                // Look for existing share and populate permissions if there one already exists

                const existing = self.parentModel.get("policy").get("permissions").findWhere({ subject: value });

                let scopes;

                const selectPermission = self.$el.find("#selectPermission select");

                if (existing) {
                    scopes = existing.get("scopes").pluck("name");
                    selectPermission[0].selectize.focus();
                    selectPermission[0].selectize.setValue(scopes);
                }

                self.enableOrDisableShareButton();
            }
        });
    },
    reset () {
        this.$el.find("#selectUser select")[0].selectize.clear();
        this.$el.find("#selectPermission select")[0].selectize.clear();
        this.$el.find("input#shareButton").prop("disabled", true);

        this.renderShareCounter();
    },
    save () {
        const permissions = this.parentModel.get("policy").get("permissions");

        const subjects = this.$el.find("#selectUser select")[0].selectize.getValue();

        const scopes = _.each(this.$el.find("#selectPermission select")[0].selectize.getValue(), (scope) => {
            return UMAPolicyPermissionScope.find({ id: scope });
        });

        const newPermissions = [];

        _.forEach(subjects, (subject) => {
            const permission = UMAPolicyPermission.findOrCreate({
                subject,
                scopes
            });
            permissions.add(permission);
            newPermissions.push(permission);
        });

        const policy = this.parentModel.get("policy");
        if (this.toBeCreated) {
            policy.toBeCreated = this.toBeCreated;
        }
        policy.save().then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedSuccess");
            this.reset();
        }, (response) => {
            if (response.status && response.status === 500) {
                _.forEach(newPermissions, (permission) => {
                    permissions.remove(permission);
                });
            }
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedFail");
        }).always(() => {
            this.toBeCreated = false;
            if (this.afterShare) {
                this.afterShare();
            }
        });
    },

    renderShareCounter (callback) {
        const policy = this.parentModel.get("policy");

        const permissionCount = policy ? policy.get("permissions").length : 0;
        ShareCounter.render(permissionCount, callback);
    }
});

export default CommonShare;
