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

import { t } from "i18next";

import { get, getSchema, remove, update } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { SOAP_STS_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import EditSoapSTSAgentGroupTemplate from
    "templates/admin/views/realms/applications/agents/soap-sts/groups/edit/EditSoapSTSAgentGroupTemplate";

class EditSoapSTSAgentGroup extends AbstractView {
    constructor () {
        super();
        this.template = EditSoapSTSAgentGroupTemplate;
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
            getSchema(realm, SOAP_STS_AGENT),
            get(realm, SOAP_STS_AGENT, id)
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

    onSave () {
        if (!this.jsonSchemaView.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }

        const formData = this.jsonSchemaView.getData();
        update(this.realm, SOAP_STS_AGENT, formData, this.data.id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }
}

export default EditSoapSTSAgentGroup;
