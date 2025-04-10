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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import "bootstrap-tabdrop";
import "selectize";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EditPolicySetTemplate from "templates/admin/views/realms/authorization/policySets/EditPolicySetTemplate";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewPolicySetTemplate from "templates/admin/views/realms/authorization/policySets/NewPolicySetTemplate";
import PoliciesService from "org/forgerock/openam/ui/admin/services/realm/PoliciesService";
import PoliciesView from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PoliciesView";
import PolicySetModel from "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel";
import PolicySetSettingsTemplate from "templates/admin/views/realms/authorization/policySets/PolicySetSettingsTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";

export default AbstractView.extend({
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
        const policySetName = args[1];

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
                { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" }
            ];
            this.model = new PolicySetModel({ name: policySetName });
            this.listenTo(this.model, "sync", this.onModelSync);
            this.model.fetch();
        } else {
            this.data.newEntity = true;
            this.template = NewPolicySetTemplate;
            this.model = new PolicySetModel();
            this.listenTo(this.model, "sync", this.onModelSync);
            this.renderAfterSyncModel();
        }
    },

    renderAfterSyncModel () {
        this.data.entity = this.model.attributes;
        this.data.displayName = this.model.displayName;
        this.data.isReadOnly = this.data.newEntity ? false : _.isEmpty(this.data.entity.resourceTypeUuids);

        if (!this.data.entity.realm) {
            this.data.entity.realm = this.realmPath;
        }

        this.renderApplication();
    },

    renderApplication () {
        const self = this;
        const parentRenderCallback = function () {
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
        };
        const populateAvailableResourceTypes = function (resourceTypes) {
            const options = {};

            options.allResourceTypes = resourceTypes;
            options.availableResourceTypes = _.filter(resourceTypes, (item) => {
                return !_.includes(self.data.entity.resourceTypeUuids, item.uuid);
            });

            options.selectedResourceTypes = _.findByValues(options.allResourceTypes, "uuid",
                self.data.entity.resourceTypeUuids);

            options.selectedResourceTypesInitial = _.clone(options.selectedResourceTypes);

            return options;
        };

        if (this.model.id) {
            this.resourceTypesPromise.then((resourceTypes) => {
                _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes.result) });
                parentRenderCallback();
            });
        } else {
            // Fill in the necessary information about application
            Promise.all([
                this.envConditionsPromise,
                this.subjConditionsPromise,
                this.decisionCombinersPromise,
                this.resourceTypesPromise
            ]).then(([envConditions, subjConditions, decisionCombiners, resourceTypes]) => {
                self.data.entity.applicationType = self.APPLICATION_TYPE;
                self.processConditions(self.data, envConditions.result, subjConditions.result);
                self.data.entity.entitlementCombiner = decisionCombiners.result.title;
                _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes.result) });
                parentRenderCallback();
            });
        }
    },

    populateResourceTypes () {
        const self = this;

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
        const result = [];
        _.each(available, (cond) => {
            result.push(cond.title);
        });
        return result;
    },

    submitForm (e) {
        e.preventDefault();

        const self = this;
        const nonModifiedAttributes = _.clone(this.model.attributes);

        this.updateFields();
        this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

        _.extend(this.model.attributes, this.data.entity);
        const savePromise = this.model.save();

        if (savePromise) {
            savePromise.then(() => {
                if (self.data.newEntity) {
                    Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                        args: _.map([self.realmPath, self.model.id], encodeURIComponent),
                        trigger: true
                    });
                } else {
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
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
        const self = this;
        const onSuccess = function () {
            Router.routeTo(Router.configuration.routes.realmsPolicySets, {
                args: [encodeURIComponent(self.realmPath)],
                trigger: true
            });
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        };
        const onError = function (model, response) {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
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
