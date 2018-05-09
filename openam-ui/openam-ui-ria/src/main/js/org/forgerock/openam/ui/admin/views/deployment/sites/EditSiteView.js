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
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/global/SitesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "templates/admin/views/deployment/sites/EditSiteTemplate"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, SitesService, FlatJSONSchemaView, FormHelper,
    Backlink, EditSiteTemplate) => { // eslint-disable-line padded-blocks

    function toggleSave (el, enable) {
        el.find("[data-save]").prop("disabled", !enable);
    }

    const EditSitesView = AbstractView.extend({
        template: EditSiteTemplate,
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        },

        render (args) {
            this.data.id = args[0];
            SitesService.sites.get(this.data.id).then((data) => {
                this.data.name = data.values.raw._id;
                this.data.etag = data.values.raw.etag;
                this.data.headerActions = [{
                    actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times"
                }];

                this.parentRender(() => {
                    new Backlink().render();
                    if (this.jsonSchemaView) {
                        this.jsonSchemaView.remove();
                    }
                    this.jsonSchemaView = new FlatJSONSchemaView({
                        schema: data.schema,
                        values: data.values,
                        onRendered: () => toggleSave(this.$el, true)
                    });
                    $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
                });
            });
        },

        onSave () {
            SitesService.sites.update(this.data.id, this.jsonSchemaView.getData(), this.data.etag)
                .then(
                    () => EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved"),
                    (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER })
                );
        },

        onDelete (event) {
            event.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.common.confirmDeleteText", { type: $.t("console.sites.common.confirmType") })
            }, () => {
                SitesService.sites.remove(this.data.id, this.data.etag).then(
                    () => Router.routeTo(Router.configuration.routes.listSites, { args: [], trigger: true }),
                    (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER })
                );
            });
        }
    });

    return new EditSitesView();
});
