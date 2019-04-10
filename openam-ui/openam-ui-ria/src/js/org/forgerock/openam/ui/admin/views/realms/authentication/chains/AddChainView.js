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
 * Copyright 2016-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AddChainTemplate from "templates/admin/views/realms/authentication/chains/AddChainTemplate";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";

function validateChainProps () {
    var name = this.$el.find("[data-chain-name]").val().trim(),
        nameExists,
        isValid;

    nameExists = _.find(this.data.chainsData, { _id:name });
    if (nameExists) {
        Messages.addMessage({
            type: Messages.TYPE_DANGER,
            message: $.t("console.authentication.chains.duplicateChain")
        });
    }
    isValid = name && !nameExists;
    this.$el.find("[data-save]").attr("disabled", !isValid);
}

export default AbstractView.extend({
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
