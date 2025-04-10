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
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import EditServiceTemplate from "templates/admin/views/realms/services/EditServiceTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServicesService from "org/forgerock/openam/ui/admin/services/realm/ServicesService";
import SubSchemaListTemplate from "templates/admin/views/realms/services/SubSchemaListTemplate";

export default AbstractView.extend({
    render ([realmPath, type]) {
        const editComponent = new EditSchemaComponent({
            data: {
                headerActions: [{
                    actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
                }],
                realmPath,
                type,
                objectName: "service"
            },
            listRoute: Router.configuration.routes.realmsServices,
            listRouteArgs: [encodeURIComponent(realmPath)],

            template: EditServiceTemplate,
            subSchemaTemplate: SubSchemaListTemplate,

            getInstance: () => ServicesService.instance.get(realmPath, type),
            updateInstance: (values) => ServicesService.instance.update(realmPath, type, values),
            deleteInstance: () => ServicesService.instance.remove(realmPath, type),

            getSubSchemaTypes: () => ServicesService.type.subSchema.type.getAll(realmPath, type),
            getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.getCreatables(realmPath, type),
            getSubSchemaInstances: () => ServicesService.type.subSchema.instance.getAll(realmPath, type),
            deleteSubSchemaInstance: (subSchemaType, subSchemaInstance) =>
                ServicesService.type.subSchema.instance.remove(realmPath, type, subSchemaType, subSchemaInstance)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
});
