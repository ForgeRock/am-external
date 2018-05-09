/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceView
  */
define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate",
    "templates/admin/views/configuration/EditGlobalConfigurationTemplate",
    "templates/admin/views/configuration/global/SubSchemaListTemplate"
], (AbstractView, UIUtils, ServicesService, Backlink, EditSchemaComponent, EditGlobalConfigurationBaseTemplate,
    EditGlobalConfigurationTemplate, SubSchemaListTemplate) => {
    const EditGlobalServiceView = AbstractView.extend({
        template: EditGlobalConfigurationBaseTemplate,
        render ([serviceType]) {
            const editComponent = new EditSchemaComponent({
                data: { serviceType },

                template: EditGlobalConfigurationTemplate,
                subSchemaTemplate: SubSchemaListTemplate,

                getInstance: () => ServicesService.instance.get(serviceType),
                updateInstance: (values) => ServicesService.instance.update(serviceType, values),

                getSubSchemaTypes: () => ServicesService.type.subSchema.type.getAll(serviceType),
                getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.getCreatables(serviceType),
                getSubSchemaInstances: () => ServicesService.type.subSchema.instance.getAll(serviceType),
                deleteSubSchemaInstance: (subSchemaType, subSchemaInstance) =>
                    ServicesService.type.subSchema.instance.remove(serviceType, subSchemaType, subSchemaInstance)
            });

            this.parentRender(() => {
                new Backlink().render();
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalServiceView();
});
