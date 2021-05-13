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
 * Copyright 2016-2019 ForgeRock AS.
 */

import { t } from "i18next";
import _ from "lodash";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import HeaderFormTemplate from "templates/admin/views/common/HeaderFormTemplate";
import InlineEditTable from "org/forgerock/openam/ui/common/components/table/InlineEditTable";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PanelComponent from "org/forgerock/openam/ui/common/components/PanelComponent";
import PartialBasedView from "org/forgerock/openam/ui/common/components/PartialBasedView";
import ServersService from "org/forgerock/openam/ui/admin/services/global/ServersService";
import setFocusToFoundInput from "org/forgerock/openam/ui/admin/utils/form/setFocusToFoundInput";
import TabComponent from "org/forgerock/openam/ui/common/components/TabComponent";
import TabSearch from "org/forgerock/openam/ui/admin/views/common/TabSearch";

function createTabs (schema) {
    return _(schema.raw.properties)
        .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
        .sortBy("order")
        .value();
}
function isAdvancedSection (sectionId) {
    return sectionId === ServersService.servers.ADVANCED_SECTION;
}

export default AbstractView.extend({
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

        this.data.title = t(`console.navigation.${this.sectionId}.title`);

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
                message: t("common.form.validation.errorsNotSaved"),
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
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
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
