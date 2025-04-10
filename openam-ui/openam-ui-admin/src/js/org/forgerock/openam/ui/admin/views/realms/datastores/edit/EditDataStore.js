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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import $ from "jquery";
import { t } from "i18next";
import { get as getValues, getSchema, update, remove, getTypeDisplayName, loadSchema } from
    "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditDataStoreTemplate from "templates/admin/views/realms/datastores/EditDataStoreTemplate";
import DataStoresFooterTemplate from "templates/admin/views/realms/datastores/_DataStoresFooter";

const dataStoresFooter = "templates/admin/views/realms/datastores/_DataStoresFooter";
class EditDataStore extends AbstractView {
    constructor () {
        super();
        this.partials = {
            [dataStoresFooter]: DataStoresFooterTemplate
        };
    }

    render ([realm, type, id]) {
        const editComponent = new EditSchemaComponent({
            data: {
                id,
                headerActions: [{
                    actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
                }],
                objectName: "identityStore"
            },
            footer: "templates/admin/views/realms/datastores/_DataStoresFooter",
            listRoute: Router.configuration.routes.realmsDataStores,
            listRouteArgs: [encodeURIComponent(realm)],
            template: EditDataStoreTemplate,

            getInstance: () => Promise.all([
                getSchema(realm, type),
                getValues(realm, type, id),
                getTypeDisplayName(realm, type)
            ]).then(([schema, values, typeDisplayName]) => {
                return {
                    id,
                    name: typeDisplayName ? typeDisplayName : type,
                    schema: new JSONSchema(schema),
                    values: new JSONValues(values)
                };
            }),
            updateInstance: (values) => {
                return update(realm, JSON.parse(values)).then(() => {
                    if (this.$el.find("[data-load-schema]").prop("checked")) {
                        return loadSchema(realm, JSON.parse(values)).then(() => {
                            Messages.addMessage({ message: t("config.messages.AdminMessages.schemaLoaded") });
                            this.$el.find("[data-load-schema]").prop("checked", false);
                        }, (response) => {
                            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                        });
                    }
                    return $.Deferred().resolve();
                });
            },
            deleteInstance: (values) => remove(realm, [values.raw])
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditDataStore;
