/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { t } from "i18next";

import { get, getSchema, remove, update } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { SOFTWARE_PUBLISHER } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditSoftwarePublisherAgentGroupTemplate from "templates/admin/views/realms/applications/agents/software-publisher/groups/edit/EditSoftwarePublisherAgentGroupTemplate"; // eslint-disable-line max-len

class EditSoftwarePublisherAgentGroup extends AbstractView {
    constructor () {
        super();
        this.template = EditSoftwarePublisherAgentGroupTemplate;
        this.events = {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        };
    }

    render ([realm, id]) {
        this.realm = realm;
        this.data = {
            id,
            headerActions: [{
                actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
            }]
        };
        Promise.all([
            getSchema(realm, SOFTWARE_PUBLISHER),
            get(realm, SOFTWARE_PUBLISHER, id)
        ]).then(([schema, values]) => {
            return {
                schema: new JSONSchema(schema[0]),
                values: new JSONValues(values[0])
            };
        }).then((data) => {
            this.parentRender(() => {
                this.jsonSchemaView = new FlatJSONSchemaView(data);
                this.jsonSchemaView.setElement("[data-json-form]"); this.jsonSchemaView.render();
            });
        });
    }

    onSave () {
        if (!this.jsonSchemaView.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }

        const formData = this.jsonSchemaView.getData();
        update(this.realm, SOFTWARE_PUBLISHER, formData, this.data.id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    onDelete () {
        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            remove(this.realm, SOFTWARE_PUBLISHER, [this.data.id]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });

                Router.routeTo(Router.configuration.routes.realmsApplicationsAgentsSoftwarePublisher, {
                    args: [encodeURIComponent(this.realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
}

export default EditSoftwarePublisherAgentGroup;
