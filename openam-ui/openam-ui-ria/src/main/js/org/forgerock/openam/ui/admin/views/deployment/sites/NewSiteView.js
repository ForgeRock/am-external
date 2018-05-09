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
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/SitesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "templates/admin/views/deployment/sites/NewSiteTemplate"
], ($, _, Messages, AbstractView, Router, SitesService, FlatJSONSchemaView, Backlink, NewSiteTemplate) => {
    const NewSiteView = AbstractView.extend({
        template: NewSiteTemplate,
        events: {
            "click [data-create]": "onCreate",
            "keyup  [data-site-name]": "onValidateProps"
        },

        render () {
            SitesService.sites.getInitialState().then((data) => this.parentRender(() => {
                new Backlink().render();
                this.jsonSchemaView = new FlatJSONSchemaView({
                    schema: data.schema,
                    values: data.values
                });
                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-site-form]"));
            }));
        },

        onCreate () {
            const values = _.cloneDeep(this.jsonSchemaView.getData());
            const siteId = this.$el.find("[data-site-name]").val();
            values["_id"] = siteId;

            // This doesn't have the values.removeNullPasswords fix from OPENAM-11834 as not required for this view.
            // However it might need adding in the future.
            SitesService.sites.create(values)
                .then(() => { Router.routeTo(Router.configuration.routes.listSites, { args: [], trigger: true }); },
                    (response) => { Messages.addMessage({ response, type: Messages.TYPE_DANGER }); }
                );
        },

        onValidateProps (event) {
            let siteId = $(event.currentTarget).val();
            if (siteId.indexOf(" ") !== -1) {
                siteId = false;
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    message: $.t("console.sites.new.nameValidationError")
                });
            }
            this.$el.find("[data-create]").prop("disabled", !siteId);
        }
    });

    return new NewSiteView();
});
