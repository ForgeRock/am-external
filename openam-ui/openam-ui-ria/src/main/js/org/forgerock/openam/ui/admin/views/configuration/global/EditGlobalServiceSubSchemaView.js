/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate",
    "templates/admin/views/common/schema/EditServiceSubSchemaTemplate",
    "templates/admin/views/configuration/global/SubSubSchemaListTemplate"
], ($, AbstractView, Router, ServicesService, Backlink, EditSchemaComponent, EditGlobalConfigurationBaseTemplate,
    EditServiceSubSchemaTemplate, SubSubSchemaListTemplate) => {
    const EditGlobalServiceSubSchemaView = AbstractView.extend({
        template: EditGlobalConfigurationBaseTemplate,
        render ([serviceType, subSchemaType, subSchemaInstanceId]) {
            // global script types can not be deleted
            const showDeleteButton = () => serviceType !== "scripting";

            const editComponent = new EditSchemaComponent({
                data: {
                    serviceType,
                    subSchemaType,
                    subSchemaInstanceId,
                    type: $.t("console.services.subSchema.title", { subSchema: subSchemaType }),
                    headerActions: showDeleteButton() ? [
                        { actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon:"fa-times" }
                    ] : []
                },
                listRoute: Router.configuration.routes.editGlobalService,
                listRouteArgs: [encodeURIComponent(serviceType)],

                template: EditServiceSubSchemaTemplate,
                subSchemaTemplate: SubSubSchemaListTemplate,

                getInstance: () => ServicesService.type.subSchema.instance.get(
                    serviceType, subSchemaType, subSchemaInstanceId),
                updateInstance: (values) => ServicesService.type.subSchema.instance.update(
                    serviceType, subSchemaType, subSchemaInstanceId, values),
                deleteInstance: () => ServicesService.type.subSchema.instance.remove(
                    serviceType, subSchemaType, subSchemaInstanceId),

                getSubSchemaTypes: () => ServicesService.type.subSchema.type.subSchema.type.getAll(
                    serviceType, subSchemaType),
                getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.subSchema.type.getCreatables(
                    serviceType, subSchemaType, subSchemaInstanceId),
                getSubSchemaInstances: () => ServicesService.type.subSchema.type.subSchema.instance.getAll(
                    serviceType, subSchemaType, subSchemaInstanceId)
            });

            this.parentRender(() => {
                new Backlink().render(2);
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalServiceSubSchemaView();
});
