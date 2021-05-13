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
 * Copyright 2015-2020 ForgeRock AS.
 */

import { each } from "lodash";
import $ from "jquery";

import { show as showDeleteDialog } from "components/dialogs/Delete";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import ChainsTemplate from "templates/admin/views/realms/authentication/ChainsTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

const getChainNameFromElement = (element) => $(element).data().chainName;

const ChainsView = AbstractView.extend({
    template: ChainsTemplate,
    events: {
        "change input[data-chain-name]" : "chainSelected",
        "click  [data-delete-chain]"    : "onDeleteSingle",
        "click  [data-delete-chains]"   : "onDeleteMultiple",
        "click  [data-select-all]"      : "selectAll"
    },
    chainSelected (event) {
        const hasChainsSelected = this.$el.find("input[type=checkbox][data-chain-name]").is(":checked");
        const row = $(event.currentTarget).closest("tr");
        const checked = $(event.currentTarget).is(":checked");

        this.$el.find("[data-delete-chains]").prop("disabled", !hasChainsSelected);

        if (checked) {
            row.addClass("selected");
        } else {
            row.removeClass("selected");
            this.$el.find("[data-select-all]").prop("checked", false);
        }
    },
    selectAll (event) {
        const checked = $(event.currentTarget).is(":checked");
        this.$el.find(".sorted-chains input[type=checkbox][data-chain-name]:not(:disabled)")
            .prop("checked", checked);
        if (checked) {
            this.$el.find(".sorted-chains:not(.default-config-row)").addClass("selected");
        } else {
            this.$el.find(".sorted-chains").removeClass("selected");
        }
        this.$el.find("[data-delete-chains]").prop("disabled", !checked);
    },
    onDeleteSingle (event) {
        event.preventDefault();

        const ids = [getChainNameFromElement(event.currentTarget)];

        this.deleteChains(ids);
    },
    onDeleteMultiple (event) {
        event.preventDefault();

        const ids = this.$el
            .find(".sorted-chains input[type=checkbox][data-chain-name]:checked")
            .map((index, element) => getChainNameFromElement(element))
            .toArray();

        this.deleteChains(ids);
    },
    deleteChains (ids) {
        showDeleteDialog({
            names: ids,
            objectName: "chain",
            onConfirm: async () => {
                await AuthenticationService.authentication.chains.remove(this.data.realmPath, ids);
                this.render([this.data.realmPath]);
            }
        });
    },
    render (args, callback) {
        const sortedChains = [];

        this.data.realmPath = args[0];

        AuthenticationService.authentication.chains.all(this.data.realmPath).then((data) => {
            each(data.values.result, (obj) => {
                // Add default chains to top of list.
                if (obj.active) {
                    sortedChains.unshift(obj);
                } else {
                    sortedChains.push(obj);
                }
            });
            this.data.sortedChains = sortedChains;
            this.parentRender(() => {
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

export default ChainsView;
