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
    "org/forgerock/openam/ui/admin/views/common/schema/NewSchemaComponent",
    "templates/admin/views/common/schema/NewServiceSubSchemaTemplate"
], ($, _, AbstractView, Router, ServicesService, NewSchemaComponent,
    NewServiceSubSchemaTemplate) => AbstractView.extend({
    render ([realmPath, serviceInstance, subSchemaType]) {
        const newSchemaComponent = new NewSchemaComponent({
            data: {
                realmPath,
                serviceInstance,
                subSchemaType,
                title: $.t("console.services.subSchema.new.title", { subSchema: subSchemaType })
            },

            listRoute: Router.configuration.routes.realmsServiceEdit,
            listRouteArgs: _.map([realmPath, serviceInstance], encodeURIComponent),

            editRoute: Router.configuration.routes.realmsServiceSubSchemaEdit,
            editRouteArgs: (newInstanceId) => _.map([realmPath, serviceInstance, subSchemaType, newInstanceId],
                encodeURIComponent),

            template: NewServiceSubSchemaTemplate,

            getInitialState: () => ServicesService.type.subSchema.instance.getInitialState(
                realmPath, serviceInstance, subSchemaType),
            createInstance: (values) => ServicesService.type.subSchema.instance.create(
                realmPath, serviceInstance, subSchemaType, values)
        });

        this.parentRender(() => { this.$el.append(newSchemaComponent.render().$el); });
    }
}));
