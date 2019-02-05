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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { get, getSchema, remove, update } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { JAVA_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditJavaAgentGroupTemplate from
    "templates/admin/views/realms/applications/agents/java/groups/edit/EditJavaAgentGroupTemplate";

class EditJavaAgentGroup extends AbstractView {
    render ([realm, id]) {
        const editComponent = new EditSchemaComponent({
            data: {
                id,
                headerActions:
                    [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
            },
            listRoute: Router.configuration.routes.realmsApplicationsAgentsJava,
            listRouteArgs: [encodeURIComponent(realm)],
            template: EditJavaAgentGroupTemplate,

            getInstance: () => Promise.all([
                getSchema(realm, JAVA_AGENT),
                get(realm, JAVA_AGENT, id)
            ]).then(([schema, values]) => {
                return {
                    schema: new JSONSchema(schema),
                    values: new JSONValues(values)
                };
            }),
            updateInstance: (values) => update(realm, JAVA_AGENT, JSON.parse(values), id),
            deleteInstance: () => remove(realm, JAVA_AGENT, [id])
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditJavaAgentGroup;