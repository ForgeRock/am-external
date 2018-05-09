/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PoliciesView",
    "org/forgerock/openam/ui/admin/services/realm/PoliciesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "partials/util/_HelpLink",
    "templates/admin/views/realms/authorization/policySets/EditPolicySetTemplate",
    "templates/admin/views/realms/authorization/policySets/NewPolicySetTemplate",
    "templates/admin/views/realms/authorization/policySets/PolicySetSettingsTemplate",
    "bootstrap-tabdrop",
    "selectize"
], ($, _, PolicySetModel, StripedListView, PoliciesView, PoliciesService, FormHelper, Messages, AbstractView,
    EventManager, Router, Constants, HelpLinkPartial, EditPolicySetTemplate, NewPolicySetTemplate,
    PolicySetSettingsTemplate) => {
    return AbstractView.extend({
        partials: {
            "util/_HelpLink": HelpLinkPartial
        },
        APPLICATION_TYPE: "iPlanetAMWebAgentService",
        validationFields: ["name", "resourceTypeUuids"],
        events: {
            "click [data-save]": "submitForm",
            "click [data-delete]": "onDeleteClick"
        },

        initialize () {
            AbstractView.prototype.initialize.call(this);
            this.model = null;
        },

        onModelSync () {
            this.renderAfterSyncModel();
        },

        render (args, callback) {
            var policySetName = args[1];

            this.realmPath = args[0];

            if (callback) {
                this.renderCallback = callback;
            }

            this.appTypePromise = PoliciesService.getApplicationType(this.APPLICATION_TYPE);
            this.envConditionsPromise = PoliciesService.getEnvironmentConditions();
            this.subjConditionsPromise = PoliciesService.getSubjectConditions();
            this.decisionCombinersPromise = PoliciesService.getDecisionCombiners();
            this.resourceTypesPromise = PoliciesService.listResourceTypes();

            if (policySetName) {
                this.template = EditPolicySetTemplate;
                this.data.headerActions = [
                    { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" },
                    { actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policySets" }
                ];
                this.model = new PolicySetModel({ name: policySetName });
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.data.newEntity = true;
                this.template = NewPolicySetTemplate;
                this.model = new PolicySetModel();
                this.data.headerActions = [{ actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policySets" }];
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel () {
            this.data.entity = this.model.attributes;
            this.data.displayName = this.model.displayName;

            if (!this.data.entity.realm) {
                this.data.entity.realm = this.realmPath;
            }

            this.renderApplication();
        },

        renderApplication () {
            var self = this,
                parentRenderCallback = function () {
                    self.parentRender(() => {
                        PoliciesView.render({
                            realmPath: self.realmPath,
                            policySetModel: self.model
                        }, () => {
                            self.$el.find(".tab-menu .nav-tabs").tabdrop();
                            const tpl = PolicySetSettingsTemplate(self.data);
                            self.$el.find("#policySetSettings").append(tpl);
                            self.populateResourceTypes();
                            FormHelper.setActiveTab(self);
                            self.$el.find("#policySetSettings [autofocus]").focus();
                            if (self.renderCallback) {
                                self.renderCallback();
                            }
                        });
                    });
                },
                populateAvailableResourceTypes = function (resourceTypes) {
                    var options = {};

                    options.allResourceTypes = resourceTypes;
                    options.availableResourceTypes = _.filter(resourceTypes, (item) => {
                        return !_.contains(self.data.entity.resourceTypeUuids, item.uuid);
                    });

                    options.selectedResourceTypes = _.findByValues(options.allResourceTypes, "uuid",
                        self.data.entity.resourceTypeUuids);

                    options.selectedResourceTypesInitial = _.clone(options.selectedResourceTypes);

                    return options;
                };

            if (this.model.id) {
                this.resourceTypesPromise.done((resourceTypes) => {
                    _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes.result) });
                    parentRenderCallback();
                });
            } else {
                // Fill in the necessary information about application
                $.when(this.appTypePromise, this.envConditionsPromise, this.subjConditionsPromise,
                    this.decisionCombinersPromise, this.resourceTypesPromise)
                    .done((appType, envConditions, subjConditions, decisionCombiners, resourceTypes) => {
                        self.data.entity.applicationType = self.APPLICATION_TYPE;
                        self.processConditions(self.data, envConditions[0].result, subjConditions[0].result);
                        self.data.entity.entitlementCombiner = decisionCombiners[0].result[0].title;
                        _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes[0].result) });
                        parentRenderCallback();
                    });
            }
        },

        populateResourceTypes () {
            var self = this;

            this.resTypesSelection = this.$el.find("#resTypesSelection").selectize({
                sortField: "name",
                valueField: "uuid",
                labelField: "name",
                searchField: "name",
                options: this.data.options.allResourceTypes,
                onChange (value) {
                    self.data.entity.resourceTypeUuids = value;
                }
            });
        },

        processConditions (data, envConditions, subjConditions) {
            if (!data.entityId) {
                data.entity.conditions = this.populateConditions(envConditions, envConditions);
                data.entity.subjects = this.populateConditions(subjConditions, subjConditions);
            }
        },

        populateConditions (selected, available) {
            var result = [];
            _.each(available, (cond) => {
                result.push(cond.title);
            });
            return result;
        },

        submitForm (e) {
            e.preventDefault();

            var self = this,
                savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes);

            this.updateFields();
            this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

            _.extend(this.model.attributes, this.data.entity);
            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(() => {
                        if (self.data.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                                args: _.map([self.realmPath, self.model.id], encodeURIComponent),
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

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.authorization.common.policySet") },
                _.bind(this.deletePolicySet, this));
        },

        deletePolicySet () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsPolicySets, {
                        args: [encodeURIComponent(self.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response) {
                    Messages.addMessage({
                        response,
                        type: Messages.TYPE_DANGER
                    });
                };

            this.model.destroy({
                success: onSuccess,
                error: onError,
                wait: true
            });
        },

        updateFields () {
            const dataFields = this.$el.find("[data-field]");

            _.each(dataFields, (field) => {
                const dataField = field.getAttribute("data-field");

                if (field.type === "checkbox") {
                    if (field.checked) {
                        this.data.entity[dataField].push(field.value);
                    }
                } else {
                    this.data.entity[dataField] = _.trim(field.value);
                }
            });
        }
    });
});
