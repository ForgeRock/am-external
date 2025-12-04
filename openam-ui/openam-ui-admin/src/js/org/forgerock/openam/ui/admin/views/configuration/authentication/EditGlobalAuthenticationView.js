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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/global/AuthenticationService";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import EditGlobalConfigurationBaseTemplate from
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate";
import EditGlobalConfigurationTemplate from "templates/admin/views/configuration/EditGlobalConfigurationTemplate";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";

/**
  * @module org/forgerock/openam/ui/admin/views/configuration/authentication/EditGlobalAuthenticationView
  */

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

export default new EditGlobalAuthenticationView();
