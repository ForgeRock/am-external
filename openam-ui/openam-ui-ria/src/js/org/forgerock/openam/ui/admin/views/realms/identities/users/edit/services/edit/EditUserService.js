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
 * Copyright 2018 ForgeRock AS.
 */

import { findWhere, map, result } from "lodash";
import { t } from "i18next";

import { getAllTypes, getSchema, get as getUserService, remove, update } from
    "org/forgerock/openam/ui/admin/services/realm/identities/UsersServicesService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditUserServiceTemplate from "templates/admin/views/realms/identities/users/services/EditUserServiceTemplate";

class EditUserService extends AbstractView {
    constructor () {
        super();

        this.template = EditUserServiceTemplate;

        this.events = {
            "click [data-delete]": "onDelete",
            "click [data-save]": "onSave"
        };
    }

    render ([realm, userId, type]) {
        this.data = {
            id: userId,
            headerActions: [{
                actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
            }]
        };
        this.realm = realm;
        this.type = type;

        Promise.all([
            getSchema(realm, type, userId),
            getUserService(realm, type, userId),
            getAllTypes(realm, userId)
        ]).then(([schema, service, serviceTypes]) => {
            this.schema = new JSONSchema(schema);
            this.values = new JSONValues(service);
            this.data.type = t("console.identities.users.edit.services.edit.subtitle", {
                type: result(findWhere(serviceTypes, { "_id": this.type }), "name")
            });

            this.parentRender(() => {
                this.view = new FlatJSONSchemaView({
                    schema: this.schema,
                    values: this.values
                });
                this.view.setElement("[data-json-form]");
                this.view.render();
            });
        });
    }

    onSave () {
        if (!this.view.isValid()) {
            Messages.addMessage({
                message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER
            });
            return;
        }

        this.values = this.values.extend(this.view.getData());

        update(this.realm, this.type, this.data.id, this.values).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    onDelete () {
        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            remove(this.realm, this.data.id, [this.type]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
                Router.routeTo(Router.configuration.routes.realmsIdentitiesUsersEdit, {
                    args: map([this.realm, this.data.id], encodeURIComponent),
                    trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
}

export default EditUserService;
