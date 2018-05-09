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
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/Promise",
    "templates/admin/views/realms/services/ServicesTemplate",
    "partials/util/_HelpLink"
], ($, _, Messages, AbstractView, ServicesService, Router, FormHelper, Promise, ServicesTemplate, HelpLinkPartial) => {
    function getServiceIdFromElement (element) {
        return $(element).closest("tr").data("serviceId");
    }
    function getServices (realmPath) {
        return ServicesService.instance.getAll(realmPath);
    }
    function getCreatables (realmPath) {
        return ServicesService.type.getCreatables(realmPath);
    }
    function deleteServices (ids) {
        FormHelper.showConfirmationBeforeDeleting({
            message: $.t("console.services.list.confirmDeleteSelected", { count: ids.length })
        }, () => {
            ServicesService.instance.remove(this.data.realmPath, ids).then(() => {
                this.rerender();
            }, (response) => {
                Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                this.rerender();
            });
        });
    }
    const HEADER_ACTIONS = [
        { actionPartial: "util/_HelpLink", helpLink: "backstage.config.services" }
    ];
    const ServicesView = AbstractView.extend({
        template: ServicesTemplate,
        partials: {
            "util/_HelpLink": HelpLinkPartial
        },
        events: {
            "change [data-select-service]": "serviceSelected",
            "click [data-delete-service]":  "onDeleteSingle",
            "click [data-delete-services]": "onDeleteMultiple",
            "click [data-add-service]":     "onAddService"
        },
        serviceSelected (event) {
            const anyServicesSelected = this.$el.find("input[type=checkbox]").is(":checked");
            const row = $(event.currentTarget).closest("tr");

            row.toggleClass("selected");
            this.$el.find("[data-delete-services]").prop("disabled", !anyServicesSelected);
        },
        onAddService (event) {
            event.preventDefault();
            Router.routeTo(Router.configuration.routes.realmsServiceNew, {
                args: [encodeURIComponent(this.data.realmPath)],
                trigger: true
            });
        },
        onDeleteSingle (event) {
            event.preventDefault();
            const id = getServiceIdFromElement(event.currentTarget);
            _.bind(deleteServices, this)([id]);
        },
        onDeleteMultiple (event) {
            event.preventDefault();

            const ids = _(this.$el.find("input[type=checkbox]:checked")).toArray().map(getServiceIdFromElement).value();

            _.bind(deleteServices, this)(ids);
        },
        validateAddButton (creatables) {
            if (!_.isEmpty(creatables)) {
                return;
            }
            this.$el.find("[data-add-service]").addClass("disabled").popover({
                trigger : "hover",
                container : "body",
                placement : "top",
                content: $.t("console.services.edit.unavaliable")
            });
        },
        render (args, callback) {
            this.data.args = args;
            this.data.realmPath = args[0];
            this.data.headerActions = HEADER_ACTIONS;

            Promise.all([getServices(this.data.realmPath), getCreatables(this.data.realmPath)]).then((data) => {
                const services = data[0];
                const creatables = data[1];

                this.data.services = services;
                this.parentRender(() => {
                    this.validateAddButton(creatables);
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        rerender () {
            this.render(this.data.args);
        }
    });

    return ServicesView;
});
