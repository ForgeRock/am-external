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
 * Copyright 2014-2017 ForgeRock AS.
 */

define([
    "backbone",
    "components/PanelFooter",
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/reactify",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/models/authorization/PolicyModel",
    "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel",
    "org/forgerock/openam/ui/admin/services/realm/PoliciesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/CustomResponseAttributesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/StaticResponseAttributesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/SubjectResponseAttributesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageEnvironmentsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageSubjectsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/CreatedResourcesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PolicyActionsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PolicySettingsModal",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/SummaryView",
    "org/forgerock/openam/ui/common/util/react/unmountAt",
    "react",
    "bootstrap-tabdrop",
    "selectize"
], (Backbone, PanelFooter, $, _, Messages, AbstractView, EventManager, Router, Constants, reactify, UIUtils,
    PolicyModel, PolicySetModel, PoliciesService, FormHelper, CustomResponseAttributesView,
    StaticResponseAttributesView, SubjectResponseAttributesView, ManageEnvironmentsView, ManageSubjectsView,
    CreatedResourcesView, PolicyActionsView, PolicySettingsModal, SummaryView, unmountAt, React) =>
    AbstractView.extend({
        partials: [
            "partials/util/_HelpLink.html"
        ],
        validationFields: ["name", "resources"],
        events: {
            "click [data-delete]": "onDeleteClick",
            "click [data-properties]": "openSettingsModal",
            "click [data-save]": "saveTab",
            "shown.bs.tab": "renderFooter"
        },

        getAllResponseAttributes () {
            this.model.attributes.resourceAttributes = _.union(
                this.staticAttrsView.getGroupedData(),
                SubjectResponseAttributesView.getAttrs(),
                CustomResponseAttributesView.getAttrs());
        },

        tabs: [
            { }, // summary panel
            { name: "resources", attr: ["resourceTypeUuid", "resources"] },
            { name: "actions", attr: ["actionValues"] },
            { name: "subjects", attr: ["subject"] },
            { name: "environments", attr: ["condition"] },
            { name: "responseAttributes", action: "getAllResponseAttributes" }
        ],

        initialize () {
            AbstractView.prototype.initialize.call(this);
            UIUtils.preloadPartial(
                "templates/admin/views/realms/authorization/policies/_PolicyOperationsDropDown.html");
        },

        render (args, callback) {
            const policyName = args[2];

            if (callback) {
                this.renderCallback = callback;
            }

            this.data.realmPath = args[0];
            this.data.policySetName = args[1];

            // This piece of information is necessary both when creating new and editing existing policy
            this.policySetModelPromise = new PolicySetModel({ name: this.data.policySetName }).fetch();
            this.resourceTypesPromise = PoliciesService.listResourceTypes();

            if (policyName) {
                this.allSubjectsPromise = PoliciesService.getSubjectConditions();
                this.allEnvironmentsPromise = PoliciesService.getEnvironmentConditions();
                this.allUserAttributesPromise = PoliciesService.getAllUserAttributes();
                this.data.headerActions = [
                    { actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policies" },
                    { actionPartial: "templates/admin/views/realms/authorization/policies/_PolicyOperationsDropDown" }
                ];
                this.template = "templates/admin/views/realms/authorization/policies/EditPolicyTemplate.html";
                this.model = new PolicyModel({ name: policyName });
                this.listenTo(this.model, "sync", this.renderPolicy);
                this.model.fetch();
            } else {
                this.template = "templates/admin/views/realms/authorization/policies/NewPolicyTemplate.html";
                this.newEntity = true;
                this.model = new PolicyModel();
                this.data.headerActions = [{ actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policies" }];
                this.listenTo(this.model, "sync", this.renderPolicy);
                this.renderPolicy();
            }
        },

        renderPolicy () {
            var self = this;

            this.data.entity = _.cloneDeep(this.model.attributes);
            // this line is needed for the correctly saving policy
            this.data.entity.applicationName = self.data.policySetName;
            this.data.options = {};
            this.data.status = {};

            if (this.data.entity.active) {
                this.data.status.text = "common.user.active";
                this.data.status.icon = "fa-check-circle";
                this.data.status.class = "text-success";
            } else {
                this.data.status.text = "common.user.inactive";
                this.data.status.icon = "fa-ban";
                this.data.status.class = "text-warning";
            }

            if (self.newEntity) {
                $.when(this.policySetModelPromise, this.resourceTypesPromise).done(
                    (policySetModel, resourceTypes) => {
                        self.data.options.availableResourceTypes = _.filter(resourceTypes[0].result,
                            (item) => _.contains(policySetModel[0].resourceTypeUuids, item.uuid));
                        self.parentRender(() => { self.buildResourceTypeSelection(); });
                    });
            } else {
                $.when(
                    this.policySetModelPromise,
                    this.allSubjectsPromise,
                    this.allEnvironmentsPromise,
                    this.allUserAttributesPromise,
                    this.resourceTypesPromise
                ).done((policySetModel, allSubjects, allEnvironments, allUserAttributes, resourceTypes) => {
                    const policySet = policySetModel[0];

                    self.data.options.availableResourceTypes = _.filter(resourceTypes[0].result,
                        (item) => _.contains(policySet.resourceTypeUuids, item.uuid));

                    self.staticAttributes = _.where(self.model.attributes.resourceAttributes, { type: "Static" });
                    self.userAttributes = _.where(self.model.attributes.resourceAttributes, { type: "User" });
                    self.customAttributes = _.difference(self.model.attributes.resourceAttributes,
                        self.staticAttributes, self.userAttributes);
                    self.allUserAttributes = _.sortBy(allUserAttributes[0].result);

                    self.data.options.availableEnvironments =
                        _.findByValues(allEnvironments[0].result, "title", policySet.conditions);
                    self.data.options.availableSubjects =
                        _.findByValues(allSubjects[0].result, "title", policySet.subjects);

                    const resourceType = _.find(self.data.options.availableResourceTypes, {
                        uuid: self.model.attributes.resourceTypeUuid
                    });

                    self.data.options.availableActions = self.getAvailableActionsForResourceType(resourceType);
                    self.data.options.availablePatterns = resourceType.patterns;

                    // Model.save() triggers this renderPolicy, so we need to manually clean up any previous react elements
                    unmountAt.default(this.$el.find("#summaryTab")[0]);
                    unmountAt.default(this.$el.find("#policyFooter")[0]);

                    self.parentRender(() => {
                        self.$el.find(".tab-menu .nav-tabs").tabdrop();
                        self.buildResourceTypeSelection();

                        reactify(React.createElement(SummaryView.default, {
                            ...self.data.entity,
                            onClick: (sectionName) => { this.onSummarySectionClick(sectionName); },
                            staticAttributes: self.staticAttributes,
                            customAttributes: self.customAttributes,
                            userAttributes: self.userAttributes
                        }), this.$el.find("#summaryTab"));

                        ManageSubjectsView.render(self.data);
                        ManageEnvironmentsView.render(self.data);

                        PolicyActionsView.render(self.data);
                        CreatedResourcesView.render(self.data);

                        self.staticAttrsView = new StaticResponseAttributesView({
                            staticAttributes: self.staticAttributes,
                            el: "[data-static-attributes]"
                        });
                        self.staticAttrsView.render();

                        SubjectResponseAttributesView.render([self.userAttributes, self.allUserAttributes]);
                        CustomResponseAttributesView.render(self.customAttributes);

                        FormHelper.setActiveTab(self);

                        if (self.renderCallback) {
                            self.renderCallback();
                        }
                    });
                });
            }
        },

        onSummarySectionClick (sectionName) {
            this.$el.find(`.tab-menu li a:contains("${sectionName}")`).tab("show");
        },

        openSettingsModal (e) {
            e.preventDefault();

            reactify(React.createElement(PolicySettingsModal.default, {
                data: {
                    active: this.data.entity.active,
                    description: this.data.entity.description,
                    name: this.data.entity.name
                },
                handleSubmit: this.updateAndSaveModel.bind(this)
            }), this.$el.find("#settingsModal"));
        },

        buildResourceTypeSelection () {
            var self = this;
            this.$el.find("#resTypesSelection").selectize({
                sortField: "name",
                valueField: "uuid",
                labelField: "name",
                searchField: "name",
                options: self.data.options.availableResourceTypes,
                onChange (value) {
                    self.changeResourceType(value);
                }
            });
        },

        getAvailableActionsForResourceType (resourceType) {
            var availableActions = [];
            if (resourceType) {
                _.each(resourceType.actions, function (val, key) {
                    availableActions.push({ action: key, value: val });
                });
            }
            return availableActions;
        },

        changeResourceType (value) {
            this.data.entity.resourceTypeUuid = value;

            var resourceType = _.find(this.data.options.availableResourceTypes, { uuid: value });

            this.data.options.availableActions = this.getAvailableActionsForResourceType(resourceType);
            this.data.options.availablePatterns = resourceType ? resourceType.patterns : [];

            this.data.options.newPattern = null;
            this.data.entity.resources = [];
            this.data.entity.actionValues = {};

            CreatedResourcesView.render(this.data);

            if (!this.newEntity) {
                PolicyActionsView.render(this.data);
            }
        },

        updateFields () {
            var app = this.data.entity,
                dataFields = this.$el.find("[data-field]"),
                dataField;

            _.each(dataFields, function (field) {
                dataField = field.getAttribute("data-field");

                if (field.type === "checkbox") {
                    app[dataField] = field.checked;
                } else {
                    app[dataField] = field.value;
                }
            });
        },

        updateModel () {
            this.updateFields();
            this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

            if (this.newEntity) {
                _.extend(this.model.attributes, this.data.entity);
            } else {
                const activeTabIndex = this.$el.find(".tab-pane.active").index();
                const activeTab = this.tabs[activeTabIndex];

                if (activeTab.action) {
                    this[activeTab.action]();
                }

                if (activeTab.attr) {
                    const activeTabProperties = _.pick(this.data.entity, this.tabs[activeTabIndex].attr);
                    _.extend(this.model.attributes, activeTabProperties);
                }
            }
        },

        saveTab () {
            this.updateModel();
            this.saveModel();
        },

        updateAndSaveModel (settings) {
            _.extend(this.model.attributes, settings);
            this.saveModel();
        },

        saveModel () {
            const savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(() => {
                        if (this.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsPolicyEdit, {
                                args: _.map([this.data.realmPath, this.data.policySetName, this.model.id],
                                    encodeURIComponent),
                                trigger: true
                            });
                        } else {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                        }
                    });
            } else {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        onDeleteClick (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.authorization.common.policy") },
                _.bind(this.deletePolicy, this));
        },

        deletePolicy () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                        args: _.map([self.data.realmPath, self.data.policySetName], encodeURIComponent),
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

        renderFooter () {
            const summaryTabSelected = this.$el.find(".tab-pane.active").index() === 0;
            const footer = this.$el.find("#policyFooter");

            if (summaryTabSelected) {
                unmountAt.default(footer[0]);
            } else {
                reactify(React.createElement(PanelFooter.default), footer);
            }
        }
    })
);
