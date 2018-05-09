/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent
 */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ReactAdapterView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/common/TabSearch",
    "org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent",
    "org/forgerock/openam/ui/admin/views/configuration/global/scripting/ScriptsList",
    "org/forgerock/openam/ui/common/components/PanelComponent",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "partials/form/_JSONSchemaFooter"
], ($, _, Backbone, Messages, AbstractView, EventManager, ReactAdapterView, Router, Constants, UIUtils, FormHelper,
    TabSearch, SubSchemaListComponent, ScriptsList, PanelComponent, PartialBasedView, TabComponent, JSONSchema,
    JSONValues, Promise, FlatJSONSchemaView, JSONSchemaFooterPartial) => { // eslint-disable-line padded-blocks

    ScriptsList = ScriptsList.default;

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

    return Backbone.View.extend({
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

            Promise.all(serviceCalls).then((response) => {
                const instance = response[0];
                const subSchema = response[1];

                this.data.schema = instance.schema;
                this.data.values = instance.values.revertFalseCollections(instance.schema);

                this.data.name = instance.name;
                if (!this.data.id) {
                    this.data.id = instance.id;
                }

                const tabs = createTabs(instance.schema, subSchema, isScriptingSubSchemaView(this.data.subSchemaType,
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
});