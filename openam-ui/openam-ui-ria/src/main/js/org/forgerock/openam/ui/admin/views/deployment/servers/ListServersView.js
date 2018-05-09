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
    "org/forgerock/openam/ui/admin/services/global/ServersService",
    "org/forgerock/openam/ui/common/components/TemplateBasedView",
    "org/forgerock/openam/ui/admin/views/common/ToggleCardListView",
    "templates/admin/views/deployment/servers/ListServersTemplate",
    "partials/util/_ButtonLink",
    "templates/admin/views/deployment/servers/_ServerCard",
    "templates/admin/views/deployment/servers/ServersCardsTemplate",
    "templates/admin/views/deployment/servers/ServersTableTemplate"
], ($, _, AbstractView, showConfirmationBeforeAction, Messages, ServersService, TemplateBasedView,
    ToggleCardListView, ListServersTemplate, ButtonLinkPartial, ServerCardPartial, ServersCardsTemplate,
    ServersTableTemplate) => { // eslint-disable-line padded-blocks

    showConfirmationBeforeAction = showConfirmationBeforeAction.default;

    const ListServersView = AbstractView.extend({
        template: ListServersTemplate,
        events: {
            "click [data-delete-item]" : "onDelete"
        },
        partials: {
            "util/_ButtonLink": ButtonLinkPartial,
            "templates/admin/views/deployment/servers/_ServerCard": ServerCardPartial
        },
        onDelete (event) {
            event.preventDefault();
            const id = $(event.currentTarget).data().deleteItem;
            showConfirmationBeforeAction({
                message: $.t("console.common.confirmDeleteText", { type: $.t("console.servers.common.confirmType") })
            },
            () => {
                ServersService.servers.remove(id).then(() => {
                    this.render();
                }, (response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
            });
        },
        renderToggleView (data) {
            const tableData = {
                "headers": [$.t("console.servers.list.table.0"), $.t("console.servers.list.table.1")],
                "items" : data
            };

            this.toggleView = new ToggleCardListView({
                el: "#toggleCardList",
                activeView: this.toggleView ? this.toggleView.getActiveView() : ToggleCardListView.DEFAULT_VIEW,
                button: {
                    href: "#deployment/servers/new",
                    icon: "fa-plus",
                    title: $.t("console.servers.list.new"),
                    btnClass: "btn-primary"
                }
            });

            this.toggleView.render((toggleView) => {
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementA(),
                    template: ServersCardsTemplate,
                    callback: () => {
                        this.$el.find('[data-toggle="popover"]').popover();
                    }
                }).render();
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementB(),
                    template: ServersTableTemplate
                }).render();
            });
        },

        showCallToAction () {
            this.$el.find(".call-to-action-block").removeClass("hidden");
        },

        render (args, callback) {
            ServersService.servers.getAll().then((data) => {
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

    return new ListServersView();
});
