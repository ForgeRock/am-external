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
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/NewSchemaComponent",
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate",
    "templates/admin/views/common/schema/NewServiceSubSchemaTemplate"
], ($, _, AbstractView, Router, ServicesService, Backlink, NewSchemaComponent, EditGlobalConfigurationBaseTemplate,
    NewServiceSubSchemaTemplate) => {
    const NewGlobalServiceSubSchemaView = AbstractView.extend({
        template: EditGlobalConfigurationBaseTemplate,
        render ([serviceInstance, subSchemaType]) {
            const newSchemaComponent = new NewSchemaComponent({
                data: {
                    serviceInstance,
                    subSchemaType,
                    title: $.t("console.services.subSchema.new.title", { subSchema: subSchemaType })
                },

                listRoute: Router.configuration.routes.editGlobalService,
                listRouteArgs: [encodeURIComponent(serviceInstance)],

                editRoute: Router.configuration.routes.globalServiceSubSchemaEdit,
                editRouteArgs: (newInstanceId) => _.map([serviceInstance, subSchemaType, newInstanceId],
                    encodeURIComponent),

                template: NewServiceSubSchemaTemplate,

                getInitialState: () => ServicesService.type.subSchema.instance.getInitialState(
                    serviceInstance, subSchemaType),
                createInstance: (values) => ServicesService.type.subSchema.instance.create(
                    serviceInstance, subSchemaType, values)
            });

            this.parentRender(() => {
                new Backlink().render();
                this.$el.find("[data-global-configuration]").append(newSchemaComponent.render().$el);
            });
        }
    });

    return new NewGlobalServiceSubSchemaView();
});
