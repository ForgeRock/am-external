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
 * Copyright 2015-2019 ForgeRock AS.
 */

import _ from "lodash";
import { t } from "i18next";
import $ from "jquery";

import {
    getAll as getAllRealms,
    remove as removeRealm,
    update as updateRealm
} from "org/forgerock/openam/ui/admin/services/global/RealmsService";
import { populateRealmsDropdown } from "org/forgerock/openam/ui/common/util/NavigationHelper";
import { show as showDeleteDialog } from "components/dialogs/Delete";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ButtonLinkPartial from "partials/util/_ButtonLink";
import ListRealmsTemplate from "templates/admin/views/realms/ListRealmsTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import RealmCardPartial from "templates/admin/views/realms/_RealmCard";
import RealmsCardsTemplate from "templates/admin/views/realms/RealmsCardsTemplate";
import RealmsTableTemplate from "templates/admin/views/realms/RealmsTableTemplate";
import StatusPartial from "partials/util/_Status";
import TemplateBasedView from "org/forgerock/openam/ui/common/components/TemplateBasedView";
import ToggleCardListView from "org/forgerock/openam/ui/admin/views/common/ToggleCardListView";
import updateDefaultServerAdvancedFqdnMap from
    "org/forgerock/openam/ui/admin/views/realms/updateDefaultServerAdvancedFqdnMap";

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

        const realm = this.getRealmFromEvent(event);

        if (realm.isTopLevelRealm) {
            return false;
        }

        showDeleteDialog({
            names: [realm.name],
            objectName: "realm",
            onConfirm: () => this.performDeleteRealm(realm)
        });
    }
    getRealmFromEvent (event) {
        const path = $(event.currentTarget).closest("div[data-realm-path]").data("realm-path");
        const realm = _.find(this.data.realms, { path });
        return realm;
    }
    getRealmFromList (path) {
        return _.find(this.data.realms, { path });
    }
    performDeleteRealm (realm) {
        return removeRealm(realm.path).then(() => {
            const dnsAliases = _.filter(realm.aliases, (item) => item.indexOf(".") > -1);
            updateDefaultServerAdvancedFqdnMap(realm.path, [], dnsAliases).then(() => this.render(), () => {
                this.render();
                Messages.addMessage(
                    { type: Messages.TYPE_DANGER, message: t("console.realms.edit.errors.fqdnMap") }
                );
            });
        });
    }
    isTopLevelRealm (name) {
        return name === "/";
    }
    render (args, callback) {
        const self = this;

        getAllRealms().then((data) => {
            const result = _.find(data.result, { name: "/" });

            if (result) {
                result.name = t("console.common.topLevelRealm");
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
                        t("console.realms.grid.header.0"), t("console.realms.grid.header.1"),
                        t("console.realms.grid.header.2"), t("console.realms.grid.header.3")
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
                        title: t("console.realms.newRealm")
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
        });
    }
    toggleRealmActive (event) {
        event.preventDefault();
        const self = this;
        const realm = this.getRealmFromEvent(event);

        if (realm.isTopLevelRealm) {
            return false;
        }
        realm.active = !realm.active;
        updateRealm(realm).finally(() => self.render());
    }
}

export default new ListRealmsView();
