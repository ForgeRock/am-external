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
 * Copyright 2016-2018 ForgeRock AS.
 */

import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import EditGlobalConfigurationBaseTemplate from
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import EditServiceSubSubSchemaTemplate from "templates/admin/views/common/schema/EditServiceSubSubSchemaTemplate";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";

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

export default new EditGlobalServiceSubSchemaView();
