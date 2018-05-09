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
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate",
    "templates/admin/views/common/schema/EditServiceSubSubSchemaTemplate"
], ($, AbstractView, ServicesService, Backlink, EditSchemaComponent, EditGlobalConfigurationBaseTemplate,
    EditServiceSubSubSchemaTemplate) => {
    const EditGlobalServiceSubSchemaView = AbstractView.extend({
        template: EditGlobalConfigurationBaseTemplate,
        render ([serviceType, subSchemaType, subSchemaInstanceId, subSubSchemaType, subSubSchemaInstanceId]) {
            const editComponent = new EditSchemaComponent({
                data: {
                    serviceType,
                    subSchemaType,
                    subSchemaInstanceId,
                    subSubSchemaType,
                    subSubSchemaInstanceId,
                    type: $.t("console.services.subSchema.title", { subSchema: subSubSchemaType })
                },

                template: EditServiceSubSubSchemaTemplate,

                getInstance: () => ServicesService.type.subSchema.type.subSchema.instance.get(
                    serviceType, subSchemaType, subSchemaInstanceId, subSubSchemaType, subSubSchemaInstanceId),
                updateInstance: (values) => ServicesService.type.subSchema.type.subSchema.instance.update(
                    serviceType, subSchemaType, subSchemaInstanceId, subSubSchemaType, values)
            });

            this.parentRender(() => {
                new Backlink().render(5);
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalServiceSubSchemaView();
});
