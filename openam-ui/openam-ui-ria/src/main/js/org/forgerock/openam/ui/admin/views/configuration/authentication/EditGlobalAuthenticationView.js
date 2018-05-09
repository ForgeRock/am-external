/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/views/configuration/authentication/EditGlobalAuthenticationView
  */
define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/services/global/AuthenticationService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate",
    "templates/admin/views/configuration/EditGlobalConfigurationTemplate"
], (AbstractView, UIUtils, AuthenticationService, Backlink, EditSchemaComponent, EditGlobalConfigurationBaseTemplate,
    EditGlobalConfigurationTemplate) => {
    const EditGlobalAuthenticationView = AbstractView.extend({
        template: EditGlobalConfigurationBaseTemplate,
        render ([type]) {
            const editComponent = new EditSchemaComponent({
                template: EditGlobalConfigurationTemplate,
                data: { type },
                getInstance: () => AuthenticationService.authentication.get(type),
                updateInstance: (values) => AuthenticationService.authentication.update(type, values)
            });

            this.parentRender(() => {
                new Backlink().render();
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalAuthenticationView();
});
