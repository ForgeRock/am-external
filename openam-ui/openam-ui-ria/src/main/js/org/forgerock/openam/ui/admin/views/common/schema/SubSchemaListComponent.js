/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent
  */
define("org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent", [
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, Backbone, Messages, EventManager, Constants, FormHelper, Promise) => Backbone.View.extend({
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
        Promise.all([this.getSubSchemaInstances(), this.getSubSchemaCreatableTypes()]).then((response) => {
            const html = this.subSchemaTemplate(_.assign(this.data, {
                instances: response[0],
                creatables: response[1],
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
}));
