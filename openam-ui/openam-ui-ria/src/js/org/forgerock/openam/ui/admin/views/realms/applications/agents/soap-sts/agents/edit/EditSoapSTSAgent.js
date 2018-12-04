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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { cloneDeep, get, isEmpty, mapValues, pluck, startsWith } from "lodash";
import { t } from "i18next";
import $ from "jquery";

import { get as getClient, getSchema, update, remove } from
    "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import { getAll as getAllGroups, get as getGroup } from
    "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { SOAP_STS_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditSoapSTSAgentTemplate from
    "templates/admin/views/realms/applications/agents/soap-sts/agents/edit/EditSoapSTSAgentTemplate";

const addGroupSelectionToSchema = (schema, groups) => {
    const AGENT_GROUP_PATH = "properties.agentgroup";
    const agentgroupProperty = get(schema, AGENT_GROUP_PATH);

    if (agentgroupProperty) {
        const responseIDs = pluck(groups.result, "_id");
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
        console.error("[EditSoapSTSAgent] Unable to add available SOAP sts Agent Groups to " +
            "\"properties.agentgroup\" property.");
    }
};

class EditSoapSTSAgent extends AbstractView {
    constructor () {
        super();

        this.template = EditSoapSTSAgentTemplate;
        this.events = {
            "click [data-delete]": "onDelete",
            "click [data-inherit-value]": "toggleInheritance",
            "click [data-save]": "onSave"
        };
    }

    getGroupValues (id, realm) {
        return id ? getGroup(realm, SOAP_STS_AGENT, id) : $.Deferred().resolve({});
    }

    render ([realm, id]) {
        this.data = {
            id,
            headerActions: [{
                actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
            }]
        };
        this.realm = realm;

        Promise.all([
            getSchema(realm, SOAP_STS_AGENT),
            getClient(realm, SOAP_STS_AGENT, id),
            getAllGroups(realm, SOAP_STS_AGENT)
        ]).then(([schema, values, groups]) => {
            addGroupSelectionToSchema(schema, groups);

            this.schema = new JSONSchema(schema);
            this.values = new JSONValues(values);

            const onGroupIdChange = () => {
                const hasInheritance = (schema, key) => {
                    const schemaKey = key.replace(".", ".properties.");
                    const schemaValue = get(schema.raw.properties, schemaKey);
                    return schema.hasInheritance(schemaValue);
                };
                const renderJSONSchemaView = (agentgroup, data) => {
                    const jsonSchemaView = this.view;
                    jsonSchemaView.options.hideInheritance = !agentgroup;
                    jsonSchemaView.setData(data);
                    jsonSchemaView.render();
                };
                const jsonEditorElement = this.view.subview.jsonEditor.element;
                const selectedAgentgroup = $("[name='root[agentgroup]']", jsonEditorElement).val();
                const currentAgentgroup = this.values.raw.agentgroup;

                if (selectedAgentgroup) {
                    if (selectedAgentgroup === currentAgentgroup) {
                        renderJSONSchemaView(selectedAgentgroup);
                    } else {
                        this.getGroupValues(selectedAgentgroup, realm).then((groupValues) => {
                            // Update defaults so the correct values are inserted when a user clicks an inherited button
                            this.defaultValues = new JSONValues(groupValues);

                            const newValues = mapValues(this.values.raw, (value, key) => {
                                if (startsWith(key, "_")) {
                                    return value;
                                } else if (hasInheritance(this.schema, key)) {
                                    const newValue = get(value, "inherited") ? get(groupValues, key) : value.value;

                                    return { inherited: value.inherited, value: newValue };
                                } else {
                                    return value;
                                }
                            });

                            this.values = new JSONValues(newValues).addValueForPath("agentgroup", selectedAgentgroup);
                            renderJSONSchemaView(selectedAgentgroup, this.values.raw);
                        });
                    }
                } else {
                    const newValues = mapValues(this.values.raw, (value, key) => {
                        return hasInheritance(this.schema, key) ? { inherited: false, value: value.value } : value;
                    });

                    this.values = new JSONValues(newValues).addValueForPath("agentgroup", selectedAgentgroup);
                    renderJSONSchemaView(selectedAgentgroup, this.values.raw);
                }
            };

            const groupId = get(this.values.raw, "agentgroup");
            this.getGroupValues(groupId, realm).then((groupValues) => {
                this.defaultValues = new JSONValues(groupValues);

                this.parentRender(() => {
                    const agentgroupValue = get(this.values.raw, "agentgroup");
                    const view = new FlatJSONSchemaView({
                        hideInheritance: isEmpty(agentgroupValue),
                        schema: new JSONSchema(this.schema.raw),
                        values: new JSONValues(cloneDeep(this.values.raw)),
                        onRendered: () => {
                            view.watch("root.agentgroup", onGroupIdChange);
                        }
                    });
                    this.view = view;
                    this.view.setElement("[data-json-form]");
                    this.view.render();
                });
            });
        });
    }
    updateValues () {
        this.values = this.values.extend(this.view.getData());
    }

    onSave () {
        if (!this.view.isValid()) {
            Messages.addMessage({
                message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER
            });
            return;
        }

        this.updateValues();

        const valuesWithoutNullPasswords = this.values.removeNullPasswords(this.schema);

        update(this.realm, SOAP_STS_AGENT, valuesWithoutNullPasswords.raw, this.data.id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
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
            ? this.defaultValues.raw[propertySchemaPath]
            : this.values.raw[propertySchemaPath].value;

        this.view.subview.toggleInheritance(propertySchemaPath, nextPropValue, nextInheritedValue);
        this.values = this.values.addValueForPath(propertySchemaPath, {
            inherited: nextInheritedValue,
            value: nextPropValue
        });
    }

    onDelete () {
        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            remove(this.realm, SOAP_STS_AGENT, [this.data.id]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });

                Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsSoapSTS, {
                    args: [encodeURIComponent(this.realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
}

export default EditSoapSTSAgent;
