/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
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
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/array/arrayify",
    "org/forgerock/openam/ui/common/util/Promise",
    "templates/admin/views/realms/authentication/ChainsTemplate"
], ($, _, Messages, AbstractView, AuthenticationService, FormHelper, arrayify, Promise, ChainsTemplate) => {
    function getChainNameFromElement (element) {
        return $(element).data().chainName;
    }
    function performDeleteChains (realmPath, names) {
        return Promise.all(arrayify(names).map((name) => {
            return AuthenticationService.authentication.chains.remove(realmPath, name);
        }));
    }

    var ChainsView = AbstractView.extend({
        template: ChainsTemplate,
        events: {
            "change input[data-chain-name]" : "chainSelected",
            "click  [data-delete-chain]"    : "onDeleteSingle",
            "click  [data-delete-chains]"   : "onDeleteMultiple",
            "click  [data-select-all]"      : "selectAll"
        },
        chainSelected (event) {
            var hasChainsSelected = this.$el.find("input[type=checkbox][data-chain-name]").is(":checked"),
                row = $(event.currentTarget).closest("tr"),
                checked = $(event.currentTarget).is(":checked");

            this.$el.find("[data-delete-chains]").prop("disabled", !hasChainsSelected);

            if (checked) {
                row.addClass("selected");
            } else {
                row.removeClass("selected");
                this.$el.find("[data-select-all]").prop("checked", false);
            }
        },
        selectAll (event) {
            var checked = $(event.currentTarget).is(":checked");
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

            FormHelper.showConfirmationBeforeDeleting({
                type: $.t("console.authentication.common.chain")
            }, _.bind(this.deleteChain, this, event));
        },
        onDeleteMultiple (event) {
            event.preventDefault();

            var selectedChains = this.$el.find(".sorted-chains input[type=checkbox][data-chain-name]:checked");

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.authentication.chains.confirmDeleteSelected", { count: selectedChains.length })
            }, _.bind(this.deleteChains, this, event, selectedChains));
        },
        deleteChain (event) {
            var self = this,
                element = event.currentTarget,
                name = getChainNameFromElement(event.currentTarget);

            $(element).prop("disabled", true);

            performDeleteChains(this.data.realmPath, name).then(() => {
                self.render([self.data.realmPath]);
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
                $(element).prop("disabled", false);
            });
        },
        deleteChains (event, selectedChains) {
            var self = this,
                element = event.currentTarget,
                names = _(selectedChains).toArray().map(getChainNameFromElement).value();

            $(element).prop("disabled", true);

            performDeleteChains(this.data.realmPath, names).then(() => {
                self.render([self.data.realmPath]);
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
                $(element).prop("disabled", false);
            });
        },
        render (args, callback) {
            var self = this,
                sortedChains = [];

            this.data.realmPath = args[0];

            AuthenticationService.authentication.chains.all(this.data.realmPath).then((data) => {
                _.each(data.values.result, (obj) => {
                    // Add default chains to top of list.
                    if (obj.active) {
                        sortedChains.unshift(obj);
                    } else {
                        sortedChains.push(obj);
                    }
                });
                self.data.sortedChains = sortedChains;
                self.parentRender(() => {
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

    return ChainsView;
});
