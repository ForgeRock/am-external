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
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "templates/admin/views/realms/authentication/chains/AddChainTemplate"
], ($, _, AbstractView, Router, Messages, AuthenticationService, AddChainTemplate) => {
    function validateChainProps () {
        var name = this.$el.find("[data-chain-name]").val().trim(),
            nameExists,
            isValid;

        nameExists = _.findWhere(this.data.chainsData, { _id:name });
        if (nameExists) {
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.authentication.chains.duplicateChain")
            });
        }
        isValid = name && !nameExists;
        this.$el.find("[data-save]").attr("disabled", !isValid);
    }

    return AbstractView.extend({
        template: AddChainTemplate,
        events: {
            "keyup [data-chain-name]" : "onValidateChainProps",
            "change [data-chain-name]": "onValidateChainProps",
            "click [data-save]"       : "save"
        },
        render (args, callback) {
            var self = this,
                chainsData = [];
            this.data.realmPath = args[0];

            AuthenticationService.authentication.chains.all(this.data.realmPath).then((data) => {
                _.each(data.values.result, (obj) => {
                    chainsData.push(obj);
                });
                self.data.chainsData = chainsData;

                self.parentRender(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        save () {
            var self = this,
                name = this.$el.find("[data-chain-name]").val().trim();

            AuthenticationService.authentication.chains.create(
                self.data.realmPath,
                { _id: name }
            ).then(() => {
                Router.routeTo(Router.configuration.routes.realmsAuthenticationChainEdit, {
                    args: _.map([self.data.realmPath, name], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        onValidateChainProps () {
            validateChainProps.call(this);
        }
    });
});
