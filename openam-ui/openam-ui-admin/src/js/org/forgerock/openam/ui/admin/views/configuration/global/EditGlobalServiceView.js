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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import EditGlobalConfigurationBaseTemplate from
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate";
import EditGlobalConfigurationTemplate from "templates/admin/views/configuration/EditGlobalConfigurationTemplate";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";
import SubSchemaListTemplate from "templates/admin/views/configuration/global/SubSchemaListTemplate";

/**
 * @module org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceView
 */

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

export default new EditGlobalServiceView();
