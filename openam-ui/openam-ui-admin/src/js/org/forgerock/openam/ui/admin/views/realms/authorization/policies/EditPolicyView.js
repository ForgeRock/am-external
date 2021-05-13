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
 * Copyright 2014-2019 ForgeRock AS.
 */

/* eslint-disable require-atomic-updates */

import "bootstrap-tabdrop";
import "selectize";

import { t } from "i18next";
import _ from "lodash";
import React from "react";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CreatedResourcesView from "./CreatedResourcesView";
import CustomResponseAttributesView from "./attributes/CustomResponseAttributesView";
import EditPolicyTemplate from "templates/admin/views/realms/authorization/policies/EditPolicyTemplate";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import ManageEnvironmentsView from "./conditions/ManageEnvironmentsView";
import ManageSubjectsView from "./conditions/ManageSubjectsView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewPolicyTemplate from "templates/admin/views/realms/authorization/policies/NewPolicyTemplate";
import PanelFooter from "components/PanelFooter";
import PoliciesService from "org/forgerock/openam/ui/admin/services/realm/PoliciesService";
import PolicyActionsView from "./PolicyActionsView";
import PolicyModel from "org/forgerock/openam/ui/admin/models/authorization/PolicyModel";
import PolicyOperationsDropDownPartial from
    "templates/admin/views/realms/authorization/policies/_PolicyOperationsDropDown";
import PolicySetModel from "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel";
import PolicySettingsModal from "./PolicySettingsModal";
import reactify from "org/forgerock/commons/ui/common/util/reactify";
import Router from "org/forgerock/commons/ui/common/main/Router";
import StaticResponseAttributesView from "./attributes/StaticResponseAttributesView";
import SubjectResponseAttributesView from "./attributes/SubjectResponseAttributesView";
import SummaryView from "./summary/SummaryView";
import unmountAt from "org/forgerock/openam/ui/common/util/react/unmountAt";

export default AbstractView.extend({
    partials: {
        "templates/admin/views/realms/authorization/policies/_PolicyOperationsDropDown":
            PolicyOperationsDropDownPartial
    },
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
                { actionPartial: "templates/admin/views/realms/authorization/policies/_PolicyOperationsDropDown" }
            ];
            this.template = EditPolicyTemplate;
            this.model = new PolicyModel({ name: policyName });
            this.listenTo(this.model, "sync", this.renderPolicy);
            this.model.fetch();
        } else {
            this.template = NewPolicyTemplate;
            this.newEntity = true;
            this.model = new PolicyModel();
            this.listenTo(this.model, "sync", this.renderPolicy);
            this.renderPolicy();
        }
    },

    async renderPolicy () {
        const self = this;

        this.data.entity = _.cloneDeep(this.model.attributes);
        // this line is needed for the correctly saving policy
        this.data.entity.applicationName = self.data.policySetName;
        this.data.options = {};
        this.data.status = {};

        if (this.data.entity.active) {
            this.data.status.text = "common.form.active";
            this.data.status.icon = "fa-check-circle";
            this.data.status.class = "text-success";
        } else {
            this.data.status.text = "common.form.inactive";
            this.data.status.icon = "fa-ban";
            this.data.status.class = "text-warning";
        }

        if (self.newEntity) {
            const [policySet, resourceTypes] =
                await Promise.all([
                    this.policySetModelPromise,
                    this.resourceTypesPromise
                ]);

            self.data.options.availableResourceTypes = _.filter(resourceTypes.result,
                (item) => _.includes(policySet.resourceTypeUuids, item.uuid));
            self.parentRender(() => { self.buildResourceTypeSelection(); });
        } else {
            const [policySet, allSubjects, allEnvironments, allUserAttributes, resourceTypes] =
                await Promise.all([
                    this.policySetModelPromise,
                    this.allSubjectsPromise,
                    this.allEnvironmentsPromise,
                    this.allUserAttributesPromise,
                    this.resourceTypesPromise
                ]);

            self.data.options.availableResourceTypes = _.filter(resourceTypes.result,
                (item) => _.includes(policySet.resourceTypeUuids, item.uuid));

            self.staticAttributes = _.filter(self.model.attributes.resourceAttributes, { type: "Static" });
            self.userAttributes = _.filter(self.model.attributes.resourceAttributes, { type: "User" });
            self.customAttributes = _.difference(self.model.attributes.resourceAttributes,
                self.staticAttributes, self.userAttributes);
            self.allUserAttributes = _.sortBy(allUserAttributes.result);

            self.data.options.availableEnvironments =
                _.findByValues(allEnvironments.result, "title", policySet.conditions);
            self.data.options.availableSubjects =
                _.findByValues(allSubjects.result, "title", policySet.subjects);

            const resourceType = _.find(self.data.options.availableResourceTypes, {
                uuid: self.model.attributes.resourceTypeUuid
            });

            this.data.isReadOnly = !resourceType;

            if (resourceType) {
                self.data.options.availableActions = self.getAvailableActionsForResourceType(resourceType);
                self.data.options.availablePatterns = resourceType.patterns;
            }

            // Model.save() triggers this renderPolicy, so we need to manually clean up any previous react elements
            unmountAt(this.$el.find("#summaryTab")[0]);
            unmountAt(this.$el.find("#policyFooter")[0]);

            self.parentRender(() => {
                if (resourceType) {
                    self.$el.find(".tab-menu .nav-tabs").tabdrop();
                    self.buildResourceTypeSelection();

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
                }

                const view = React.createElement(SummaryView, {
                    ...self.data.entity,
                    onClick: (sectionName) => { this.onSummarySectionClick(sectionName); },
                    staticAttributes: self.staticAttributes,
                    customAttributes: self.customAttributes,
                    isReadOnly: self.data.isReadOnly,
                    userAttributes: self.userAttributes
                });

                reactify(view, this.$el.find("#summaryTab"));

                if (self.renderCallback) {
                    self.renderCallback();
                }
            });
        }
    },

    onSummarySectionClick (sectionName) {
        this.$el.find(`.tab-menu li a:contains("${sectionName}")`).tab("show");
    },

    openSettingsModal (e) {
        e.preventDefault();

        reactify(React.createElement(PolicySettingsModal, {
            data: {
                active: this.data.entity.active,
                description: this.data.entity.description,
                name: this.data.entity.name
            },
            isReadOnlyName: this.data.isReadOnly,
            handleSubmit: this.updateAndSaveModel.bind(this)
        }), this.$el.find("#settingsModal"));
    },

    buildResourceTypeSelection () {
        const self = this;
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
        const availableActions = [];
        if (resourceType) {
            _.each(resourceType.actions, (val, key) => {
                availableActions.push({ action: key, value: val });
            });
        }
        return availableActions;
    },

    changeResourceType (value) {
        this.data.entity.resourceTypeUuid = value;

        const resourceType = _.find(this.data.options.availableResourceTypes, { uuid: value });

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
        const app = this.data.entity;
        const dataFields = this.$el.find("[data-field]");
        let dataField;

        _.each(dataFields, (field) => {
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
            savePromise.then(() => {
                if (this.newEntity) {
                    Router.routeTo(Router.configuration.routes.realmsPolicyEdit, {
                        args: _.map([this.data.realmPath, this.data.policySetName, this.model.id],
                            encodeURIComponent),
                        trigger: true
                    });
                } else {
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                }
            });
        } else {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError); // ?
        }
    },

    onDeleteClick (e) {
        e.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({ type: t("console.authorization.common.policy") },
            _.bind(this.deletePolicy, this));
    },

    deletePolicy () {
        const self = this;
        const onSuccess = function () {
            Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                args: _.map([self.data.realmPath, self.data.policySetName], encodeURIComponent),
                trigger: true
            });
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        };
        const onError = function (model, response) {
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
            unmountAt(footer[0]);
        } else {
            reactify(React.createElement(PanelFooter), footer);
        }
    }
});
