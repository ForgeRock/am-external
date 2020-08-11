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
 * Copyright 2016-2019 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ButtonLinkPartial from "partials/util/_ButtonLink";
import ListServersTemplate from "templates/admin/views/deployment/servers/ListServersTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import ServerCardPartial from "templates/admin/views/deployment/servers/_ServerCard";
import ServersCardsTemplate from "templates/admin/views/deployment/servers/ServersCardsTemplate";
import ServersService from "org/forgerock/openam/ui/admin/services/global/ServersService";
import ServersTableTemplate from "templates/admin/views/deployment/servers/ServersTableTemplate";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import TemplateBasedView from "org/forgerock/openam/ui/common/components/TemplateBasedView";
import ToggleCardListView from "org/forgerock/openam/ui/admin/views/common/ToggleCardListView";

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

export default new ListServersView();
