/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/global/ServersService",
    "org/forgerock/openam/ui/common/components/PanelComponent",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/admin/views/common/TabSearch",
    "org/forgerock/openam/ui/common/components/table/InlineEditTable",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/admin/utils/form/setFocusToFoundInput",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "templates/admin/views/common/HeaderFormTemplate"
], ($, _, Messages, AbstractView, EventManager, Constants, ServersService, PanelComponent, PartialBasedView,
    TabComponent, TabSearch, InlineEditTable, JSONSchema, JSONValues, Promise, setFocusToFoundInput,
    FlatJSONSchemaView, HeaderFormTemplate) => {
    setFocusToFoundInput = setFocusToFoundInput.default;
    function createTabs (schema) {
        return _(schema.raw.properties)
            .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
            .sortBy("order")
            .value();
    }
    function isAdvancedSection (sectionId) {
        return sectionId === ServersService.servers.ADVANCED_SECTION;
    }

    return AbstractView.extend({
        template: HeaderFormTemplate,
        events: {
            "click [data-save]": "onSave",
            "click [data-inherit-value]": "toggleInheritance"
        },
        getJSONSchemaView () {
            return this.subview.getBody();
        },
        render ([serverId, sectionId]) {
            this.sectionId = sectionId;
            this.serverId = serverId;

            this.data.title = $.t(`console.common.navigation.${this.sectionId}`);

            ServersService.servers.getWithDefaults(this.serverId, this.sectionId).then(({ defaultValues, schema,
                values }) => {
                this.schema = schema;
                this.values = values;
                this.defaultValues = defaultValues;

                this.parentRender(() => {
                    if (isAdvancedSection(this.sectionId)) {
                        this.subview = new PanelComponent({
                            createBody: () => new InlineEditTable({ values: _.cloneDeep(this.values.raw) }),
                            createFooter: () => new PartialBasedView({ partial: "form/_JSONSchemaFooter" })
                        });
                    } else {
                        const tabs = createTabs(schema);
                        this.subview = new TabComponent({
                            tabs,
                            createBody: (id) => {
                                if (schema.raw.properties[id].type === "array") {
                                    return new InlineEditTable({
                                        values: _.cloneDeep(this.values.raw)[id],
                                        rowSchema: schema.raw.properties[id].items
                                    });
                                } else {
                                    return new FlatJSONSchemaView({
                                        schema: new JSONSchema(schema.raw.properties[id]),
                                        values: new JSONValues(_.cloneDeep(this.values.raw)[id])
                                    });
                                }
                            },
                            createFooter: () => new PartialBasedView({ partial: "form/_JSONSchemaFooter" })
                        });
                        const options = {
                            properties: this.schema.raw.properties,
                            onChange: (id, value) => {
                                this.subview.setTabId(id);
                                setFocusToFoundInput(this.$el.find(`[data-schemapath="root.${value}"]`));
                            }
                        };
                        this.$el.find("[data-tab-search]").append(new TabSearch(options).render().$el);
                    }
                    this.subview.setElement("[data-json-form]");
                    this.subview.render();
                });
            });
        },
        getSection () {
            return this.sectionId === ServersService.servers.ADVANCED_SECTION
                ? this.sectionId
                : this.subview.getTabId();
        },
        updateValues () {
            this.values = this.values.extend({
                [this.getSection()]: this.getJSONSchemaView().getData()
            });
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

            const values = isAdvancedSection(this.sectionId)
                ? this.values
                : this.values.removeNullPasswords(this.schema);

            ServersService.servers.update(
                this.sectionId,
                values.raw,
                this.serverId
            ).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        },
        toggleInheritance (event) {
            const target = event.currentTarget;
            const removeJSONSchemaRootPrefix = (key) => key.slice(5);
            const propertySchemaPath = removeJSONSchemaRootPrefix(target.getAttribute("data-schemapath"));
            const isInherited = target.getAttribute("data-inherit-value") === "true";
            let propValue;

            if (isInherited) {
                propValue = this.values.raw[this.subview.getTabId()][propertySchemaPath].value;
            } else {
                propValue = this.defaultValues.raw[this.subview.getTabId()][propertySchemaPath];
            }

            this.getJSONSchemaView().subview.toggleInheritance(propertySchemaPath, propValue, !isInherited);
        }
    });
});
