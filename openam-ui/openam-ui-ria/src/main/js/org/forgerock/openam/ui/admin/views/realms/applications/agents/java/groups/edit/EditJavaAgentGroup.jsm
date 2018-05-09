/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { get, getSchema, remove, update } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { JAVA_AGENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
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
                    schema: new JSONSchema(schema[0]),
                    values: new JSONValues(values[0])
                };
            }),
            updateInstance: (values) => update(realm, JAVA_AGENT, JSON.parse(values), id),
            deleteInstance: () => remove(realm, JAVA_AGENT, [id])
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditJavaAgentGroup;
