/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
import Promise from "org/forgerock/openam/ui/common/util/Promise";
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
                headerActions:
                [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
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
                    schema: new JSONSchema(schema[0]),
                    values: new JSONValues(values[0])
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
