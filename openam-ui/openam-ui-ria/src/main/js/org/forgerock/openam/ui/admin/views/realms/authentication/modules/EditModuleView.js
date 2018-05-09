/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent",
    "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate"
], (AbstractView, Router, AuthenticationService, EditSchemaComponent, EditModuleViewTemplate) => AbstractView.extend({
    render ([realmPath, moduleType, moduleName]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                moduleType,
                moduleName
            },
            template: EditModuleViewTemplate,
            getInstance: () => AuthenticationService.authentication.modules.get(
                realmPath, moduleName, moduleType),
            updateInstance: (values) => AuthenticationService.authentication.modules.update(
                realmPath, moduleName, moduleType, values)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}));
