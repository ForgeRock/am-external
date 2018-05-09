/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { get, getSchema, remove, update } from "org/forgerock/openam/ui/admin/services/realm/AgentGroupsService";
import { OAUTH2_CLIENT } from "org/forgerock/openam/ui/admin/services/realm/AgentTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import EditOAuth2GroupTemplate from
    "templates/admin/views/realms/applications/oauth2/groups/edit/EditOAuth2GroupTemplate";

class EditOAuth2Group extends AbstractView {
    render ([realm, id]) {
        const editComponent = new EditSchemaComponent({
            data: {
                id,
                headerActions:
                    [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
            },
            listRoute: Router.configuration.routes.realmsApplicationsOAuth2,
            listRouteArgs: [encodeURIComponent(realm)],
            template: EditOAuth2GroupTemplate,

            getInstance: () => Promise.all([
                getSchema(realm, OAUTH2_CLIENT),
                get(realm, OAUTH2_CLIENT, id)
            ]).then(([schema, values]) => {
                return {
                    schema: new JSONSchema(schema[0]),
                    values: new JSONValues(values[0])
                };
            }),
            updateInstance: (values) => update(realm, OAUTH2_CLIENT, JSON.parse(values), id),
            deleteInstance: () => remove(realm, OAUTH2_CLIENT, [id])
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditOAuth2Group;
