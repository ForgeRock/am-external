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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { chain, cloneDeep, get, isEmpty, map } from "lodash";
import { t } from "i18next";
import $ from "jquery";

import { get as getClient, getSchema, update, remove } from
    "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { getAll as getAllGroups, get as getGroup } from
    "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { removeAgentRootURLPrefix, addAgentRootURLPrefix } from
    "org/forgerock/openam/ui/admin/views/realms/applications/agents/common/agentRootUrl";
import { JAVA_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PartialBasedView from "org/forgerock/openam/ui/common/components/PartialBasedView";
import Router from "org/forgerock/commons/ui/common/main/Router";
import setFocusToFoundInput from "org/forgerock/openam/ui/admin/utils/form/setFocusToFoundInput";
import TabComponent from "org/forgerock/openam/ui/common/components/TabComponent";
import TabSearch from "org/forgerock/openam/ui/admin/views/common/TabSearch";
import EditJavaAgentTemplate from
    "templates/admin/views/realms/applications/agents/java/agents/edit/EditJavaAgentTemplate";

const CDSSO_ROOT_URL_PATH = "globalJ2EEAgentConfig.cdssoRootUrl";

const createTabs = (schema) => {
    return chain(schema.raw.properties)
        .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
        .sortBy("order")
        .value();
};

const addGroupSelectionToSchema = (schema, groups) => {
    const AGENT_GROUP_PATH = "properties.globalJ2EEAgentConfig.properties.agentgroup";
    const agentgroupProperty = get(schema, AGENT_GROUP_PATH);

    if (agentgroupProperty) {
        const responseIDs = map(groups.result, "_id");
        const availableGroupsIDs = ["", ...responseIDs];
        const availableGroupsTitles = [
            t("common.form.unassigned"),
            ...responseIDs
        ];

        // The non spec `additional` property has been added to the schema here to allow for additional properties to
        // be passed down to the JSONEditorTheme. In this case we render the descriptions with the `alert-info` property
        // as info alerts
        agentgroupProperty.additional = {
            alert: "alert-info"
        };
        agentgroupProperty.enum = availableGroupsIDs;
        agentgroupProperty.options = {
            "enum_titles": availableGroupsTitles
        };
    } else {
        console.error("[EditJavaAgent] Unable to add available Java Agent Groups to " +
            "\"globalJ2EEAgentConfig.properties.agentgroup\" property.");
    }
};

class EditJavaAgent extends AbstractView {
    constructor () {
        super();

        this.template = EditJavaAgentTemplate;
        this.events = {
            "click [data-delete]": "onDelete",
            "click [data-inherit-value]": "toggleInheritance",
            "click [data-save]": "onSave"
        };
    }

    getJSONSchemaView () {
        return this.subview.getBody();
    }

    getGroupValues (id, realm) {
        return id ? getGroup(realm, JAVA_AGENT, id) : $.Deferred().resolve({});
    }

    render ([realm, id]) {
        this.data = {
            id,
            headerActions:
                [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
        };
        this.realm = realm;

        Promise.all([
            getSchema(realm, JAVA_AGENT),
            getClient(realm, JAVA_AGENT, id),
            getAllGroups(realm, JAVA_AGENT)
        ]).then(([schema, values, groups]) => {
            addGroupSelectionToSchema(schema, groups);
            values = removeAgentRootURLPrefix(values, CDSSO_ROOT_URL_PATH);

            this.schema = new JSONSchema(schema);
            this.values = new JSONValues(values);
            this.nonInheritedValues = cloneDeep(this.values);

            const onGroupIdChange = () => {
                const hasInheritance = (schema, key) => {
                    const schemaKey = key.replace(".", ".properties.");
                    const schemaValue = get(schema.raw.properties, schemaKey);
                    return schema.hasInheritance(schemaValue);
                };
                const renderJSONSchemaView = (agentgroup, data) => {
                    const jsonSchemaView = this.getJSONSchemaView();
                    jsonSchemaView.options.hideInheritance = !agentgroup;
                    jsonSchemaView.setData(data);
                    jsonSchemaView.render();
                };
                const jsonEditorElement = this.getJSONSchemaView().subview.jsonEditor.element;
                const selectedAgentgroup = $("[name='root[agentgroup]']", jsonEditorElement).val();
                const currentAgentgroup = this.values.raw.globalJ2EEAgentConfig.agentgroup;

                if (selectedAgentgroup) {
                    if (selectedAgentgroup === currentAgentgroup) {
                        renderJSONSchemaView(selectedAgentgroup);
                    } else {
                        this.getGroupValues(selectedAgentgroup, realm).then((groupValues) => {
                            // Update defaults so the correct values are inserted when a user clicks an inherited button
                            this.defaultValues = new JSONValues(groupValues);

                            // Update this.values so inherited values across tabs are updated
                            this.values = this.values.mapProperties((value, key) => {
                                if (hasInheritance(this.schema, key)) {
                                    const newValue = get(value, "inherited")
                                        ? get(groupValues, key)
                                        : value.value;
                                    return {
                                        inherited: value.inherited,
                                        value: newValue
                                    };
                                } else {
                                    return value;
                                }
                            });
                            this.values.raw.globalJ2EEAgentConfig.agentgroup = selectedAgentgroup;

                            /**
                             * The group has changed so the user is on the tab where group selection takes place.
                             * Thus we need to update the data contained within the editor as well.
                             */
                            renderJSONSchemaView(selectedAgentgroup, this.values.raw[this.getSection()]);
                        });
                    }
                } else {
                    this.values = this.nonInheritedValues.mapProperties((value, key) => {
                        return hasInheritance(this.schema, key) ? { inherited: false, value: value.value } : value;
                    });
                    this.values.raw.globalJ2EEAgentConfig.agentgroup = selectedAgentgroup;
                    renderJSONSchemaView(selectedAgentgroup, this.values.raw[this.getSection()]);
                }
            };

            const groupId = get(this.values.raw, "globalJ2EEAgentConfig.agentgroup");
            this.getGroupValues(groupId, realm).then((groupValues) => {
                this.defaultValues = new JSONValues(groupValues);

                this.parentRender(() => {
                    const tabs = createTabs(this.schema);
                    this.subview = new TabComponent({
                        tabs,
                        createBody: (id) => {
                            // No inheritance informaton should be displayed when a client does not belong to a group
                            const agentgroupValue = get(this.values.raw, "globalJ2EEAgentConfig.agentgroup");
                            const view = new FlatJSONSchemaView({
                                hideInheritance: isEmpty(agentgroupValue),
                                schema: new JSONSchema(this.schema.raw.properties[id]),
                                values: new JSONValues(cloneDeep(this.values.raw)[id]),
                                onRendered: () => {
                                    view.watch("root.agentgroup", onGroupIdChange);
                                }
                            });
                            return view;
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

                    this.subview.setElement("[data-json-form]");
                    this.subview.render();
                });
            });
        });
    }

    getSection () {
        return this.subview.getTabId();
    }

    updateValues () {
        this.values = this.values.extend({
            [this.getSection()]: this.getJSONSchemaView().getData()
        });
        this.nonInheritedValues = cloneDeep(this.values);
    }

    onSave () {
        if (!this.getJSONSchemaView().isValid()) {
            Messages.addMessage({
                message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER
            });
            return;
        }

        this.updateValues();

        const valuesWithoutNullPasswords = this.values.removeNullPasswords(this.schema);
        const valuesWithAgentRootURL = addAgentRootURLPrefix(valuesWithoutNullPasswords.raw, CDSSO_ROOT_URL_PATH);

        update(this.realm, JAVA_AGENT, valuesWithAgentRootURL, this.data.id).then(() => {
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    toggleInheritance (event) {
        const target = event.currentTarget;
        const removeJSONSchemaRootPrefix = (key) => key.slice(5);
        const propertySchemaPath = removeJSONSchemaRootPrefix(target.getAttribute("data-schemapath"));
        const nextInheritedValue = !(target.getAttribute("data-inherit-value") === "true");
        const nextPropValue = nextInheritedValue
            ? this.defaultValues.raw[this.getSection()][propertySchemaPath]
            : this.nonInheritedValues.raw[this.getSection()][propertySchemaPath].value;

        if (nextInheritedValue) {
            this.nonInheritedValues.raw[this.getSection()][propertySchemaPath].value =
                this.getJSONSchemaView().getData()[propertySchemaPath].value;
        }

        this.getJSONSchemaView().subview.toggleInheritance(propertySchemaPath, nextPropValue, nextInheritedValue);
        this.values = this.values.addValueForPath(`${this.getSection()}.${propertySchemaPath}`, {
            inherited: nextInheritedValue,
            value: nextPropValue
        });
    }

    onDelete () {
        showDeleteDialog({
            names: [this.data.id],
            objectName: "agent",
            onConfirm: async () => {
                try {
                    await remove(this.realm, JAVA_AGENT, [this.data.id]);
                    Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsJava, {
                        args: [encodeURIComponent(this.realm)], trigger: true
                    });
                } catch (error) {
                    Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
                }
            }
        });
    }
}

export default EditJavaAgent;