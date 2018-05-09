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
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypePatternsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypeActionsView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "partials/util/_HelpLink",
    "templates/admin/views/realms/authorization/resourceTypes/EditResourceTypeTemplate",
    "templates/admin/views/realms/authorization/resourceTypes/NewResourceTypeTemplate",
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypeSettingsTemplate",
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, ResourceTypeModel,
    ResourceTypePatternsView, ResourceTypeActionsView, FormHelper, HelpLinkPartial,
    EditResourceTypeTemplate, NewResourceTypeTemplate, ResourceTypeSettingsTemplate) => {
    return AbstractView.extend({
        partials: {
            "util/_HelpLink": HelpLinkPartial
        },
        events: {
            "click [data-save]": "submitForm",
            "click [data-delete]": "onDeleteClick"
        },
        tabs: [
            { name: "patterns", attr: ["patterns"] },
            { name: "actions", attr: ["actions"] },
            { name: "settings", attr: ["name", "description"] }
        ],

        onModelSync () {
            this.renderAfterSyncModel();
        },

        render (args, callback) {
            var uuid;

            this.data.realmPath = args[0];
            if (callback) {
                this.renderCallback = callback;
            }

            // Realm location is the first argument, second one is the resource type uuid
            if (args.length === 2) {
                uuid = args[1];
            }

            if (uuid) {
                this.template = EditResourceTypeTemplate;
                this.data.headerActions = [
                    { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" },
                    { actionPartial: "util/_HelpLink", helpLink: "backstage.authz.resourceTypes" }
                ];
                this.model = new ResourceTypeModel({ uuid });
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.template = NewResourceTypeTemplate;
                this.data.headerActions = [
                    { actionPartial: "util/_HelpLink", helpLink: "backstage.authz.resourceTypes" }
                ];
                this.data.newEntity = true;
                this.model = new ResourceTypeModel();
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel () {
            var self = this,
                data = this.data;
            this.data.entity = _.cloneDeep(this.model.attributes);

            data.actions = [];
            _.each(this.data.entity.actions, (v, k) => {
                data.actions.push({ name: k, value: v });
            });
            data.actions.sort();

            this.initialActions = _.cloneDeep(data.actions);
            this.initialPatterns = _.cloneDeep(data.entity.patterns);

            this.parentRender(() => {
                var promises = [], resolve = function () { return (promises[promises.length] = $.Deferred()).resolve; },
                    data = self.data;

                self.$el.find(".tab-menu .nav-tabs").tabdrop();
                self.renderSettings();

                self.patternsView = new ResourceTypePatternsView();
                self.patternsView.render(data.entity, data.entity.patterns, "#resTypePatterns", resolve());

                self.actionsList = new ResourceTypeActionsView();
                self.actionsList.render(data, "#resTypeActions", resolve());

                $.when(...promises).done(() => {
                    FormHelper.setActiveTab(self);
                    if (self.renderCallback) { self.renderCallback(); }
                });
            });
        },

        renderSettings () {
            var self = this;
            const tpl = ResourceTypeSettingsTemplate(this.data);
            self.$el.find("#resTypeSetting").html(tpl);
            self.$el.find("#resTypeSetting [autofocus]").focus();
        },

        updateFields () {
            var app = this.data.entity,
                dataFields = this.$el.find("[data-field]"),
                dataField;

            _.each(dataFields, (field) => {
                dataField = field.getAttribute("data-field");

                if (field.type === "checkbox") {
                    if (field.checked) {
                        app[dataField].push(field.value);
                    }
                } else {
                    app[dataField] = field.value;
                }
            });
        },

        submitForm (e) {
            e.preventDefault();

            var self = this,
                savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes),
                activeTab = this.$el.find(".tab-pane.active"),
                activeTabProperties;

            this.updateFields();
            this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

            if (this.data.newEntity) {
                _.extend(this.model.attributes, this.data.entity);
            } else {
                activeTabProperties = _.pick(this.data.entity, this.tabs[activeTab.index()].attr);
                _.extend(this.model.attributes, activeTabProperties);
            }

            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(() => {
                        if (self.data.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                                args: _.map([self.data.realmPath, self.model.id], encodeURIComponent),
                                trigger: true
                            });
                        } else {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                        }
                    });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        onDeleteClick (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.authorization.common.resourceType") },
                _.bind(this.deleteResourceType, this));
        },

        deleteResourceType () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsResourceTypes, {
                        args: [encodeURIComponent(self.data.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response) {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                };

            this.model.destroy({
                success: onSuccess,
                error: onError,
                wait: true
            });
        }
    });
});
