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
 * Copyright 2016-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONSchemaFooterPartial from "partials/form/_JSONSchemaFooter";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PartialBasedView from "org/forgerock/openam/ui/common/components/PartialBasedView";
import ReactAdapterView from "org/forgerock/commons/ui/common/main/ReactAdapterView";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ScriptsList from "org/forgerock/openam/ui/admin/views/configuration/global/scripting/ScriptsList";
import SubSchemaListComponent from "org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent";
import TabComponent from "org/forgerock/openam/ui/common/components/TabComponent";
import TabSearch from "org/forgerock/openam/ui/admin/views/common/TabSearch";

/**
 * @module org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent
 */

const PSEUDO_TAB = { id: _.uniqueId("pseudo_tab_"), title: $.t("console.common.configuration") };
const SUBSCHEMA_TAB = { id: "subschema", title: $.t("console.common.secondaryConfigurations") };
const DEFAULT_SCRIPTS_TAB = { id: "defaultScripts", title: "Default Scripts" };

const isScriptingSubSchemaView = (subSchemaType, subSubSchemaType) => subSchemaType === "contexts" &&
    _.isEmpty(subSubSchemaType);

const createTabs = (schema, subSchemaTypes, isScriptingSubSchemaView) => {
    let tabs = [];
    const hasSubSchema = subSchemaTypes && subSchemaTypes.length > 0;
    const schemaIsCollection = schema.isCollection();

    if (schemaIsCollection) {
        tabs = tabs.concat(_(schema.raw.properties)
            .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
            .sortBy("order")
            .value());
    } else if (hasSubSchema && schema.raw.properties) {
        tabs.push(PSEUDO_TAB);
    }

    if (isScriptingSubSchemaView) {
        tabs.push(DEFAULT_SCRIPTS_TAB);
    }

    if (hasSubSchema) {
        tabs.push(SUBSCHEMA_TAB);
    }

    return tabs;
};

export default Backbone.View.extend({
    partials: {
        "form/_JSONSchemaFooter": JSONSchemaFooterPartial
    },
    events: {
        "click [data-save]": "onSave",
        "click [data-delete]": "onDelete"
    },

    initialize ({
        data,
        footer,
        listRoute,
        listRouteArgs,
        template,
        subSchemaTemplate,
        getInstance,
        updateInstance,
        deleteInstance,
        getSubSchemaTypes,
        getSubSchemaCreatableTypes,
        getSubSchemaInstances,
        deleteSubSchemaInstance
    }) {
        this.data = data;
        this.listRoute = listRoute;
        this.listRouteArgs = listRouteArgs;
        this.footer = footer || "form/_JSONSchemaFooter";
        this.template = template;
        this.subSchemaTemplate = subSchemaTemplate;

        this.getInstance = getInstance;
        this.updateInstance = updateInstance;
        this.deleteInstance = deleteInstance;

        this.getSubSchemaTypes = getSubSchemaTypes;
        this.getSubSchemaCreatableTypes = getSubSchemaCreatableTypes;
        this.getSubSchemaInstances = getSubSchemaInstances;
        this.deleteSubSchemaInstance = deleteSubSchemaInstance;
    },

    createTabComponent (tabs) {
        return new TabComponent({
            tabs,
            createBody: (id) => {
                if (id === SUBSCHEMA_TAB.id) {
                    return new SubSchemaListComponent({
                        data: this.data,
                        subSchemaTemplate: this.subSchemaTemplate,
                        getSubSchemaCreatableTypes: this.getSubSchemaCreatableTypes,
                        getSubSchemaInstances: this.getSubSchemaInstances,
                        deleteSubSchemaInstance: this.deleteSubSchemaInstance
                    });
                } else if (id === PSEUDO_TAB.id) {
                    return new FlatJSONSchemaView({
                        schema: this.data.schema,
                        values: this.data.values
                    });
                } else if (id === DEFAULT_SCRIPTS_TAB.id) {
                    return new ReactAdapterView({
                        reactView: ScriptsList,
                        reactProps: { subSchemaType: this.data.subSchemaInstanceId },
                        needsBaseTemplate: false
                    });
                } else {
                    return new FlatJSONSchemaView({
                        schema: new JSONSchema(this.data.schema.raw.properties[id]),
                        values: new JSONValues(this.data.values.raw[id])
                    });
                }
            },
            createFooter: (id) => {
                if (id !== SUBSCHEMA_TAB.id && id !== DEFAULT_SCRIPTS_TAB.id) {
                    return new PartialBasedView({ partial: this.footer });
                }
            }
        });
    },

    getJSONSchemaView () {
        return this.subview instanceof TabComponent ? this.subview.getBody() : this.subview;
    },

    render () {
        const serviceCalls =
            _([this.getInstance, this.getSubSchemaTypes])
                .compact()
                .map((serviceCall) => serviceCall())
                .value();

        /**
         * There may have been no Promise to populate subSchema, hence a default Object is applied.
         */
        Promise.all(serviceCalls).then(([instance, subSchema = {}]) => {
            this.data.schema = instance.schema;
            this.data.values = instance.values.revertFalseCollections(instance.schema);

            this.data.name = instance.name;
            if (!this.data.id) {
                this.data.id = instance.id;
            }

            const tabs = createTabs(instance.schema, subSchema.result, isScriptingSubSchemaView(this.data.subSchemaType,
                this.data.subSubSchemaType));
            const hasTabs = !_.isEmpty(tabs);

            this.data.hasTabs = hasTabs;

            this.$el.html(this.template(this.data));

            if (hasTabs) {
                this.subview = this.createTabComponent(tabs);
                if (this.data.schema.isCollection()) {
                    const options = {
                        properties: this.data.schema.raw.properties,
                        onChange: (tabId, value) => {
                            this.subview.setTabId(tabId);
                            this.$el.find(`[data-schemapath="root.${value}"]`).find("input").focus();
                        }
                    };
                    this.$el.find("[data-tab-search]").append(new TabSearch(options).render().$el);
                }
            } else {
                this.subview = new FlatJSONSchemaView({
                    schema: instance.schema,
                    values: instance.values
                });
            }

            this.subview.setElement(this.$el.find("[data-json-form]"));
            this.subview.render();
        });

        return this;
    },

    updateValues () {
        if (this.data.schema.isCollection()) {
            this.data.values = this.data.values
                .extend({ [this.subview.getTabId()]: this.getJSONSchemaView().getData() })
                .revertFalseCollections(this.data.schema);
        } else {
            this.data.values = new JSONValues(this.getJSONSchemaView().getData());
        }
    },

    onSave () {
        if (!this.getJSONSchemaView().isValid()) {
            Messages.addMessage({
                message: $.t("common.form.validation.errorsNotSaved"),
                type: Messages.TYPE_DANGER
            });
            return;
        }
        this.updateValues();
        const valuesWithoutNullPasswords = this.data.values.removeNullPasswords(this.data.schema);
        this.updateInstance(valuesWithoutNullPasswords.toJSON()).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    },

    onDelete (e) {
        e.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({
            message: $.t("console.common.confirmDeleteItem")
        }, () => {
            this.deleteInstance(this.data.values).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                Router.routeTo(this.listRoute, { args: this.listRouteArgs, trigger: true });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
});
