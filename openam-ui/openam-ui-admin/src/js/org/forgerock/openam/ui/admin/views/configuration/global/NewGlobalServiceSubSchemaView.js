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

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import EditGlobalConfigurationBaseTemplate from
    "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate";
import NewSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/NewSchemaComponent";
import NewServiceSubSchemaTemplate from "templates/admin/views/common/schema/NewServiceSubSchemaTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";

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

export default new NewGlobalServiceSubSchemaView();
