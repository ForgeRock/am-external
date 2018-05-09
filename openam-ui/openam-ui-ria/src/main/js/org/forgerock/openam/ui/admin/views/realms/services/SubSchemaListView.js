/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/Promise",
    "templates/admin/views/realms/services/SubSchemaListTemplate"
], ($, _, Backbone, Messages, EventManager, Router, Constants, ServicesService, FormHelper, Promise,
    SubSchemaListTemplate) => {
    function deleteSubSchema (realmPath, type, subSchemaType, subSchemaInstance) {
        return ServicesService.type.subSchema.instance.remove(realmPath, type, subSchemaType, subSchemaInstance);
    }

    const SubschemaListView = Backbone.View.extend({
        template: SubSchemaListTemplate,
        events: {
            "click [data-subschema-delete]" : "onDelete"
        },
        initialize (options) {
            this.options = options;
        },
        render () {
            Promise.all([
                ServicesService.type.subSchema.instance.getAll(this.options.realmPath, this.options.type),
                ServicesService.type.subSchema.type.getCreatables(this.options.realmPath, this.options.type)
            ]).then((response) => {
                const data = _.merge({}, this.options, {
                    instances: response[0][0],
                    creatables:response[1][0]
                });

                const html = this.template(data);
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
                message: $.t("console.services.subSchema.confirmDeleteSelected")
            }, () => {
                deleteSubSchema(this.options.realmPath, this.options.type, subSchemaType, subSchemaInstance)
                    .then(() => {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                        this.render();
                    }, (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
            });
        }
    });

    return SubschemaListView;
});
