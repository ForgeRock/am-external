/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/realms/services/EditServiceTemplate",
    "templates/admin/views/realms/services/SubSchemaListTemplate"
], (AbstractView, Router, ServicesService, EditSchemaComponent, EditServiceTemplate,
    SubSchemaListTemplate) => AbstractView.extend({
    render ([realmPath, type]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                type,
                headerActions:
                    [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
            },
            listRoute: Router.configuration.routes.realmsServices,
            listRouteArgs: [encodeURIComponent(realmPath)],

            template: EditServiceTemplate,
            subSchemaTemplate: SubSchemaListTemplate,

            getInstance: () => ServicesService.instance.get(realmPath, type),
            updateInstance: (values) => ServicesService.instance.update(realmPath, type, values),
            deleteInstance: () => ServicesService.instance.remove(realmPath, type),

            getSubSchemaTypes: () => ServicesService.type.subSchema.type.getAll(realmPath, type),
            getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.getCreatables(realmPath, type),
            getSubSchemaInstances: () => ServicesService.type.subSchema.instance.getAll(realmPath, type),
            deleteSubSchemaInstance: (subSchemaType, subSchemaInstance) =>
                ServicesService.type.subSchema.instance.remove(realmPath, type, subSchemaType, subSchemaInstance)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}));
