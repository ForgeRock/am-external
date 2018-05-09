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
    "org/forgerock/openam/ui/common/components/SelectComponent",
    "templates/admin/views/realms/authentication/modules/AddModuleTemplate"
], ($, _, AbstractView, Router, Messages, AuthenticationService, SelectComponent, AddModuleTemplate) => {
    SelectComponent = SelectComponent.default;

    function validateModuleProps () {
        var moduleName = this.$el.find("#newModuleName").val(),
            moduleType = this.moduleType,
            isValid;

        if (moduleName.indexOf(" ") !== -1) {
            moduleName = false;
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.authentication.modules.moduleNameValidationError")
            });
        }
        isValid = moduleName && moduleType;
        this.$el.find("[data-save]").attr("disabled", !isValid);
    }

    return AbstractView.extend({
        template: AddModuleTemplate,
        events: {
            "change [data-module-name]": "onValidateModuleProps",
            "keyup  [data-module-name]": "onValidateModuleProps",
            "change [data-module-type]": "onValidateModuleProps",
            "click [data-save]"        : "save"
        },
        render (args, callback) {
            var self = this;
            this.data.realmPath = args[0];

            AuthenticationService.authentication.modules.types.all(this.data.realmPath).then((modulesData) => {
                self.parentRender(() => {
                    const selectComponent = new SelectComponent({
                        options: modulesData.result,
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
            var self = this,
                moduleName = self.$el.find("#newModuleName").val(),
                moduleType = this.moduleType,
                modulesService = AuthenticationService.authentication.modules;

            modulesService.exists(self.data.realmPath, moduleName).then((result) => {
                var authenticationModules = modulesService;
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
});
