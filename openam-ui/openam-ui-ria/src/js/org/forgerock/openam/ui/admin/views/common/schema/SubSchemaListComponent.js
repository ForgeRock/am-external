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

import { assign, sortBy } from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
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

        FormHelper.showConfirmationBeforeDeleting({
            message: $.t("console.common.confirmDeleteItem")
        }, () => {
            this.deleteSubSchemaInstance(subSchemaType, subSchemaInstance).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                this.render();
            },
            (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
        });
    }
});