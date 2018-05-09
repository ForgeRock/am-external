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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/common/schema/EditServiceSubSchemaTemplate"
], ($, _, AbstractView, Router, ServicesService, EditSchemaComponent,
    EditServiceSubSchemaTemplate) => AbstractView.extend({
    render ([realmPath, serviceType, subSchemaType, id]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                serviceType,
                subSchemaType,
                id,
                type: $.t("console.services.subSchema.title", { subSchema: subSchemaType }),
                subSchemaInstanceId: decodeURIComponent(id),
                headerActions: [
                    { actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon:"fa-times" }
                ]
            },
            listRoute: Router.configuration.routes.realmsServiceEdit,
            listRouteArgs: _.map([realmPath, serviceType], encodeURIComponent),

            template: EditServiceSubSchemaTemplate,

            getInstance:
                () => ServicesService.type.subSchema.instance.get(realmPath, serviceType, subSchemaType, id),
            updateInstance:
                (values) => ServicesService.type.subSchema.instance.update(
                    realmPath, serviceType, subSchemaType, id, values),
            deleteInstance:
                () => ServicesService.type.subSchema.instance.remove(realmPath, serviceType, subSchemaType, id)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}));
