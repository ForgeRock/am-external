/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2017 ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent"
], (AbstractView, Router, AuthenticationService, EditSchemaComponent) => AbstractView.extend({
    render ([realmPath, moduleType, moduleName]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                moduleType,
                moduleName
            },
            template: "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate.html",
            getInstance: () => AuthenticationService.authentication.modules.get(
                realmPath, moduleName, moduleType),
            updateInstance: (values) => AuthenticationService.authentication.modules.update(
                realmPath, moduleName, moduleType, values)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}));
