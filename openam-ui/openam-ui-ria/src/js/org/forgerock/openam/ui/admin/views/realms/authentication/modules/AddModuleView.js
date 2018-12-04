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
import AddModuleTemplate from "templates/admin/views/realms/authentication/modules/AddModuleTemplate";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SelectComponent from "org/forgerock/openam/ui/common/components/SelectComponent";

function validateModuleProps () {
    const moduleType = this.moduleType;
    let moduleName = this.$el.find("#newModuleName").val();

    if (moduleName.indexOf(" ") !== -1) {
        moduleName = false;
        Messages.addMessage({
            type: Messages.TYPE_DANGER,
            message: $.t("console.authentication.modules.moduleNameValidationError")
        });
    }
    const isValid = moduleName && moduleType;
    this.$el.find("[data-save]").attr("disabled", !isValid);
}

export default AbstractView.extend({
    template: AddModuleTemplate,
    events: {
        "change [data-module-name]": "onValidateModuleProps",
        "keyup  [data-module-name]": "onValidateModuleProps",
        "change [data-module-type]": "onValidateModuleProps",
        "click [data-save]"        : "save"
    },
    render (args, callback) {
        const self = this;
        this.data.realmPath = args[0];

        AuthenticationService.authentication.modules.types.all(this.data.realmPath).then((response) => {
            self.parentRender(() => {
                const selectComponent = new SelectComponent({
                    options: response,
                    onChange: (option) => {
                        self.moduleType = option._id;
                        self.onValidateModuleProps();
                    },
                    searchFields: ["name"],
                    labelField: "name",
                    placeholderText: $.t("console.authentication.modules.selectModuleType")
                });
                self.$el.find("[data-module-type]").append(selectComponent.render().el);
                self.$el.find("[autofocus]").focus();
                if (callback) {
                    callback();
                }
            });
        });
    },
    save () {
        const self = this;
        const moduleName = self.$el.find("#newModuleName").val();
        const moduleType = this.moduleType;
        const modulesService = AuthenticationService.authentication.modules;

        modulesService.exists(self.data.realmPath, moduleName).then((result) => {
            const authenticationModules = modulesService;
            if (result) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    message: $.t("console.authentication.modules.addModuleError")
                });
            } else {
                authenticationModules.create(self.data.realmPath, { _id: moduleName }, moduleType)
                    .then(() => {
                        Router.routeTo(Router.configuration.routes.realmsAuthenticationModuleEdit, {
                            args: _.map([self.data.realmPath, moduleType, moduleName], encodeURIComponent),
                            trigger: true
                        });
                    }, (response) => {
                        Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                    });
            }
        }, (response) => {
            Messages.addMessage({ type: Messages.TYPE_DANGER, response });
        });
    },
    onValidateModuleProps () {
        validateModuleProps.call(this);
    }
});
