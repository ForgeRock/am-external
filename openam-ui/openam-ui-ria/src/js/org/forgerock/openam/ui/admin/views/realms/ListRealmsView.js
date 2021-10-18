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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import { populateRealmsDropdown } from "org/forgerock/openam/ui/common/util/NavigationHelper";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import RealmsService from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import TemplateBasedView from "org/forgerock/openam/ui/common/components/TemplateBasedView";
import ToggleCardListView from "org/forgerock/openam/ui/admin/views/common/ToggleCardListView";
import updateDefaultServerAdvancedFqdnMap
    from "org/forgerock/openam/ui/admin/views/realms/updateDefaultServerAdvancedFqdnMap";
import ListRealmsTemplate from "templates/admin/views/realms/ListRealmsTemplate";
import RealmsCardsTemplate from "templates/admin/views/realms/RealmsCardsTemplate";
import RealmsTableTemplate from "templates/admin/views/realms/RealmsTableTemplate";
import StatusPartial from "partials/util/_Status";
import ButtonLinkPartial from "partials/util/_ButtonLink";
import RealmCardPartial from "templates/admin/views/realms/_RealmCard";

class ListRealmsView extends AbstractView {
    constructor () {
        super();
        this.template = ListRealmsTemplate;
        this.events = {
            "click [data-delete-realm]" : "deleteRealm",
            "click [data-toggle-realm]" : "toggleRealmActive"
        };
        this.partials = {
            "util/_Status": StatusPartial,
            "util/_ButtonLink": ButtonLinkPartial,
            "templates/admin/views/realms/_RealmCard": RealmCardPartial
        };
    }
    deleteRealm (event) {
        event.preventDefault();

        var self = this,
            realm = this.getRealmFromEvent(event),
            buttons = [{
                label: $.t("common.form.cancel"),
                action: (dialog) => {
                    dialog.close();
                }
            }, {
                label: $.t("common.form.delete"),
                cssClass: "btn-danger",
                action: (dialog) => {
                    self.performDeleteRealm(realm).always(() => {
                        dialog.close();
                    });
                }
            }];

        if (realm.isTopLevelRealm) {
            return false;
        }

        if (realm.active) {
            buttons.splice(1, 0, {
                label: $.t("common.form.deactivate"),
                action: (dialog) => {
                    realm.active = false;
                    RealmsService.realms.update(realm).then(null, (response) => {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            response
                        });
                    }).always(() => {
                        self.render();
                        dialog.close();
                    });
                }
            });
        }

        BootstrapDialog.show({
            title: $.t("console.realms.warningDialog.title", { realmName: realm.name }),
            type: BootstrapDialog.TYPE_DANGER,
            message: realm.active ? $.t("console.realms.warningDialog.activateMessage")
                : $.t("console.realms.warningDialog.deactivateMessage"),
            buttons
        });
    }
    getRealmFromEvent (event) {
        var path = $(event.currentTarget).closest("div[data-realm-path]").data("realm-path"),
            realm = _.find(this.data.realms, { path });
        return realm;
    }
    getRealmFromList (path) {
        return _.find(this.data.realms, { path });
    }
    performDeleteRealm (realm) {
        var self = this;
        return RealmsService.realms.remove(realm.path).then(() => {
            const dnsAliases = _.filter(realm.aliases, (item) => item.indexOf(".") > -1);
            updateDefaultServerAdvancedFqdnMap(realm.path, [], dnsAliases).then(() => self.render(), () => {
                self.render();
                Messages.addMessage(
                    { type: Messages.TYPE_DANGER, message: $.t("console.realms.edit.errors.fqdnMap") }
                );
            });
        }, (response) => {
            if (response && response.status === 409) {
                Messages.addMessage({
                    message: $.t("console.realms.parentRealmCannotDeleted"),
                    type: Messages.TYPE_DANGER
                });
            } else {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            }
        });
    }
    isTopLevelRealm (name) {
        return name === "/";
    }
    render (args, callback) {
        var self = this;

        RealmsService.realms.all().then((data) => {
            var result = _.find(data.result, { name: "/" });

            if (result) {
                result.name = $.t("console.common.topLevelRealm");
            }
            self.data.realms = data.result;
            self.data.allRealmPaths = [];
            populateRealmsDropdown(data);

            _.each(self.data.realms, (realm) => {
                realm.isTopLevelRealm = self.isTopLevelRealm(realm.path);
                self.data.allRealmPaths.push(realm.path);
            });

            self.parentRender(() => {
                const tableData = {
                    "headers": [
                        $.t("console.realms.grid.header.0"), $.t("console.realms.grid.header.1"),
                        $.t("console.realms.grid.header.2"), $.t("console.realms.grid.header.3")
                    ],
                    "items" : self.data.realms
                };

                this.toggleView = new ToggleCardListView({
                    el: "#toggleCardList",
                    activeView: this.toggleView ? this.toggleView.getActiveView() : ToggleCardListView.DEFAULT_VIEW,
                    button: {
                        btnClass: "btn-primary",
                        href: "#realms/new",
                        icon: "fa-plus",
                        title: $.t("console.realms.newRealm")
                    }
                });

                this.toggleView.render((toggleView) => {
                    new TemplateBasedView({
                        data: tableData,
                        el: toggleView.getElementA(),
                        template: RealmsCardsTemplate,
                        callback: () => {
                            this.$el.find('[data-toggle="popover"]').popover();
                        }
                    }).render();
                    new TemplateBasedView({
                        data: tableData,
                        el: toggleView.getElementB(),
                        template: RealmsTableTemplate
                    }).render();
                });

                if (callback) {
                    callback();
                }
            });
        }, (response) =>
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                response
            })
        );
    }
    toggleRealmActive (event) {
        event.preventDefault();
        var self = this,
            realm = this.getRealmFromEvent(event);

        if (realm.isTopLevelRealm) {
            return false;
        }
        realm.active = !realm.active;
        RealmsService.realms.update(realm).then(null, (response) => {
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                response
            });
        }).always(() => self.render());
    }
}

export default new ListRealmsView();