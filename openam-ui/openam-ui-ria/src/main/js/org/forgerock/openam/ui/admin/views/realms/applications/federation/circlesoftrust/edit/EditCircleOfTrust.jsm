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

import { cloneDeep, get, map } from "lodash";
import { t } from "i18next";

import { get as getCircleOfTrust, getSchema, update, remove }
    from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";
import { getAll as getAllProvidersByType }
    from "org/forgerock/openam/ui/admin/services/realm/federation/EntityProviders";
import { SAML2, WS_FED } from "org/forgerock/openam/ui/admin/services/realm/federation/EntityProviderTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditCircleOfTrustTemplate from
    "templates/admin/views/realms/applications/federation/circlesoftrust/edit/EditCircleOfTrustTemplate";

const addSuffixes = (providers, suffix) => {
    return map(providers, (provider) => `${provider._id}|${suffix}`);
};

const addEntityProviders = (schema, saml2Entities, wsFedEntities) => {
    const modifiedSchema = cloneDeep(schema);
    const trustedProviderItems = get(modifiedSchema, "properties.trustedProviders.items");
    if (trustedProviderItems) {
        const entityProviderLabels = [...addSuffixes(saml2Entities, "saml2"), ...addSuffixes(wsFedEntities, "wsfed")];
        trustedProviderItems.enum = entityProviderLabels;
        trustedProviderItems.options = {
            "enum_titles": entityProviderLabels
        };
    }
    return modifiedSchema;
};

class EditCircleOfTrust extends AbstractView {
    constructor () {
        super();

        this.template = EditCircleOfTrustTemplate;
        this.events = {
            "click [data-delete]": "onDelete",
            "click [data-save]": "onSave"
        };
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
            getSchema(realm),
            getCircleOfTrust(realm, id),
            getAllProvidersByType(realm, SAML2),
            getAllProvidersByType(realm, WS_FED)
        ]).then(([schema, values, saml2Entities, wsFedEntities]) => {
            const modifiedSchema = addEntityProviders(schema[0], saml2Entities[0].result, wsFedEntities[0].result);
            return {
                schema: new JSONSchema(modifiedSchema),
                values: new JSONValues(values[0])
            };
        }).then(({ schema, values }) => {
            this.schema = schema;
            this.values = values;
            this.parentRender(() => {
                const view = new FlatJSONSchemaView({
                    hideInheritance: true,
                    schema,
                    values
                });

                this.view = view;
                this.view.setElement("[data-json-form]");
                this.view.render();
            });
        });
    }

    onSave () {
        if (!this.view.isValid()) {
            Messages.addMessage({ message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER });
            return;
        }
        this.values = this.view.getData();

        update(this.realm, this.values, this.data.id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    onDelete () {
        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            remove(this.realm, [this.data.id]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
                Router.routeTo(Router.configuration.routes.realmsApplicationsFederation, {
                    args: [encodeURIComponent(this.realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
}

export default EditCircleOfTrust;
