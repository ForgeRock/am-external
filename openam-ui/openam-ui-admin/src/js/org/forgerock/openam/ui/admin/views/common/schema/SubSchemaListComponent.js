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

import { assign, sortBy } from "lodash";
import { t } from "i18next";
import $ from "jquery";
import Backbone from "backbone";

import { show as showDeleteDialog } from "components/dialogs/Delete";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

/**
  * @module org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent
  */
export default Backbone.View.extend({
    events: {
        "click [data-subschema-delete]" : "onDelete"
    },

    initialize ({
        data,
        subSchemaTemplate,
        getSubSchemaCreatableTypes,
        getSubSchemaInstances,
        deleteSubSchemaInstance
    }) {
        this.data = data;
        this.subSchemaTemplate = subSchemaTemplate;
        this.getSubSchemaCreatableTypes = getSubSchemaCreatableTypes;
        this.getSubSchemaInstances = getSubSchemaInstances;
        this.deleteSubSchemaInstance = deleteSubSchemaInstance;
    },

    render () {
        Promise.all([
            this.getSubSchemaInstances(),
            this.getSubSchemaCreatableTypes()
        ]).then(([instances, creatables]) => {
            const html = this.subSchemaTemplate(assign(this.data, {
                instances: sortBy(instances.result, "_id"),
                creatables: sortBy(creatables.result, "name"),
                // scripting sub configuration (default types) can't be deleted
                showDeleteButton: this.data.serviceType !== "scripting"
            }));

            this.$el.html(html);
        });

        return this;
    },

    onDelete (event) {
        event.preventDefault();

        const target = $(event.currentTarget);
        const subSchemaInstance = target.closest("tr").data("subschemaId");
        const subSchemaType = target.closest("tr").data("subschemaType");

        showDeleteDialog({
            names: [subSchemaInstance],
            objectName: "configuration",
            onConfirm: () => {
                return this.deleteSubSchemaInstance(subSchemaType, subSchemaInstance).then(() => {
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                    this.render();
                },
                (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
            }
        });
    }
});
