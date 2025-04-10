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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import "bootstrap-tabdrop";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EditResourceTypeTemplate from
    "templates/admin/views/realms/authorization/resourceTypes/EditResourceTypeTemplate";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewResourceTypeTemplate from "templates/admin/views/realms/authorization/resourceTypes/NewResourceTypeTemplate";
import ResourceTypeActionsView from
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypeActionsView";
import ResourceTypeModel from "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel";
import ResourceTypePatternsView from
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypePatternsView";
import ResourceTypeSettingsTemplate from
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypeSettingsTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";

export default AbstractView.extend({
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
        let uuid;

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
                { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" }
            ];
            this.model = new ResourceTypeModel({ uuid });
            this.listenTo(this.model, "sync", this.onModelSync);
            this.model.fetch();
        } else {
            this.template = NewResourceTypeTemplate;
            this.data.newEntity = true;
            this.model = new ResourceTypeModel();
            this.listenTo(this.model, "sync", this.onModelSync);
            this.renderAfterSyncModel();
        }
    },

    renderAfterSyncModel () {
        const self = this;
        const data = this.data;
        this.data.entity = _.cloneDeep(this.model.attributes);

        data.actions = [];
        _.each(this.data.entity.actions, (v, k) => {
            data.actions.push({ name: k, value: v });
        });
        data.actions.sort();

        this.initialActions = _.cloneDeep(data.actions);
        this.initialPatterns = _.cloneDeep(data.entity.patterns);

        this.parentRender(() => {
            const promises = [];
            const resolve = function () { return (promises[promises.length] = $.Deferred()).resolve; };
            const data = self.data;

            self.$el.find(".tab-menu .nav-tabs").tabdrop();
            self.renderSettings();

            self.patternsView = new ResourceTypePatternsView();
            self.patternsView.render(data.entity, data.entity.patterns, "#resTypePatterns", resolve());

            self.actionsList = new ResourceTypeActionsView();
            self.actionsList.render(data, "#resTypeActions", resolve());

            Promise.all(promises).then(() => {
                FormHelper.setActiveTab(self);
                if (self.renderCallback) { self.renderCallback(); }
            });
        });
    },

    renderSettings () {
        const self = this;
        const tpl = ResourceTypeSettingsTemplate(this.data);
        self.$el.find("#resTypeSetting").html(tpl);
        self.$el.find("#resTypeSetting [autofocus]").focus();
    },

    updateFields () {
        const app = this.data.entity;
        const dataFields = this.$el.find("[data-field]");
        let dataField;

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

        const self = this;
        const nonModifiedAttributes = _.clone(this.model.attributes);
        const activeTab = this.$el.find(".tab-pane.active");
        let activeTabProperties;

        this.updateFields();
        this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

        if (this.data.newEntity) {
            _.extend(this.model.attributes, this.data.entity);
        } else {
            activeTabProperties = _.pick(this.data.entity, this.tabs[activeTab.index()].attr);
            _.extend(this.model.attributes, activeTabProperties);
        }

        const savePromise = this.model.save();

        if (savePromise) {
            savePromise.then(() => {
                if (self.data.newEntity) {
                    Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                        args: _.map([self.data.realmPath, self.model.id], encodeURIComponent),
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

        FormHelper.showConfirmationBeforeDeleting({ type: t("console.authorization.common.resourceType") },
            _.bind(this.deleteResourceType, this));
    },

    deleteResourceType () {
        const self = this;
        const onSuccess = function () {
            Router.routeTo(Router.configuration.routes.realmsResourceTypes, {
                args: [encodeURIComponent(self.data.realmPath)],
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
    }
});
