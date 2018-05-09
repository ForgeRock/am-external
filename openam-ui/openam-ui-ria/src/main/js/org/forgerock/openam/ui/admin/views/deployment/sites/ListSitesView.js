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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/global/SitesService",
    "org/forgerock/openam/ui/common/components/TemplateBasedView",
    "org/forgerock/openam/ui/admin/views/common/ToggleCardListView",
    "templates/admin/views/deployment/sites/ListSitesTemplate",
    "partials/util/_ButtonLink",
    "templates/admin/views/deployment/sites/_SiteCard",
    "templates/admin/views/deployment/sites/SitesCardsTemplate",
    "templates/admin/views/deployment/sites/SitesTableTemplate"
], ($, _, AbstractView, showConfirmationBeforeAction, Messages, SitesService, TemplateBasedView,
    ToggleCardListView, ListSitesTemplate, ButtonLinkPartial, SiteCardTemplate,
    SitesCardsTemplate, SitesTableTemplate) => { // eslint-disable-line padded-blocks

    showConfirmationBeforeAction = showConfirmationBeforeAction.default;

    const ListSitesView = AbstractView.extend({
        template: ListSitesTemplate,
        events: {
            "click [data-delete-item]" : "onDelete"
        },
        partials: {
            "util/_ButtonLink": ButtonLinkPartial,
            "templates/admin/views/deployment/sites/_SiteCard": SiteCardTemplate
        },

        onDelete (event) {
            event.preventDefault();
            const id = $(event.currentTarget).data().deleteItem;

            showConfirmationBeforeAction({
                message: $.t("console.common.confirmDeleteText", {
                    type: $.t("console.sites.common.confirmType")
                })
            },
            () => {
                SitesService.sites.remove(id).then(
                    () => this.render(),
                    (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER })
                );
            });
        },

        renderToggleView (data) {
            const tableData = {
                "headers": [
                    $.t("console.sites.list.table.0"), $.t("console.sites.list.table.1"),
                    $.t("console.sites.list.table.2"), $.t("console.sites.list.table.3")
                ],
                "items" : data
            };

            this.toggleView = new ToggleCardListView({
                el: "#toggleCardList",
                activeView: this.toggleView ? this.toggleView.getActiveView() : ToggleCardListView.DEFAULT_VIEW,
                button: {
                    href: "#deployment/sites/new",
                    icon: "fa-plus",
                    title: $.t("console.sites.list.new"),
                    btnClass: "btn-primary"
                }
            });

            this.toggleView.render((toggleView) => {
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementA(),
                    template: SitesCardsTemplate,
                    callback: () => {
                        this.$el.find('[data-toggle="popover"]').popover();
                    }
                }).render();
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementB(),
                    template: SitesTableTemplate
                }).render();
            });
        },

        showCallToAction () {
            this.$el.find(".call-to-action-block").removeClass("hidden");
        },

        render (args, callback) {
            SitesService.sites.getAll().then((data) => {
                this.parentRender(() => {
                    if (_.isEmpty(data)) {
                        this.showCallToAction();
                    } else {
                        this.renderToggleView(data);
                    }

                    if (callback) {
                        callback();
                    }
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        }
    });

    return new ListSitesView();
});
