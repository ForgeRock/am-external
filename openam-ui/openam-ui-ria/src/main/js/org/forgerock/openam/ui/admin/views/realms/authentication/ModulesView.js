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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/common/util/array/arrayify",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/admin/views/realms/authentication/EditModuleDialog",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "templates/admin/views/realms/authentication/ModulesTemplate",
    "partials/alerts/_Alert",

    // jquery dependencies
    "selectize"
], ($, _, AbstractView, arrayify, Configuration, EditModuleDialog, Form, FormHelper, Messages,
    Promise, AuthenticationService, ModulesTemplate, AlertPartial) => {
    function getModuleInfoFromElement (element) {
        return $(element).closest("tr").data();
    }
    function performDeleteModules (realmPath, moduleInfos) {
        return Promise.all(arrayify(moduleInfos).map((moduleInfo) => {
            return AuthenticationService.authentication.modules.remove(realmPath, moduleInfo.moduleName,
                moduleInfo.moduleType);
        }));
    }

    var ModulesView = AbstractView.extend({
        template: ModulesTemplate,
        events: {
            "change [data-select-module]"   : "moduleSelected",
            "click [data-delete-module]"    : "onDeleteSingle",
            "click [data-delete-modules]"   : "onDeleteMultiple",
            "click [data-check-before-edit]": "editModule"
        },
        partials: {
            "alerts/_Alert": AlertPartial
        },
        data: {},
        moduleSelected (event) {
            var hasModuleSelected = this.$el.find("input[type=checkbox]").is(":checked"),
                row = $(event.currentTarget).closest("tr"),
                checked = $(event.currentTarget).is(":checked");

            this.$el.find("[data-delete-modules]").prop("disabled", !hasModuleSelected);
            if (checked) {
                row.addClass("selected");
            } else {
                row.removeClass("selected");
            }
        },
        editModule (event) {
            event.preventDefault();
            var data = $(event.currentTarget).closest("tr").data(),
                href = event.currentTarget.href;

            EditModuleDialog(data.moduleName, data.moduleChains, href);
        },
        onDeleteSingle (event) {
            event.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                type: $.t("console.authentication.common.module")
            }, _.bind(this.deleteModule, this, event));
        },
        onDeleteMultiple (event) {
            event.preventDefault();

            var selectedModules = this.$el.find("input[type=checkbox]:checked");

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.authentication.modules.confirmDeleteSelected", { count: selectedModules.length })
            }, _.bind(this.deleteModules, this, event, selectedModules));
        },
        deleteModule (event) {
            var self = this,
                element = event.currentTarget,
                moduleInfo = getModuleInfoFromElement(element);

            $(element).prop("disabled", true);

            performDeleteModules(this.data.realmPath, moduleInfo).then(() => {
                self.render(self.data.args);
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
                $(element).prop("disabled", false);
            });
        },
        deleteModules (event, selectedModules) {
            var self = this,
                element = event.currentTarget,
                moduleInfos = _(selectedModules).toArray().map(getModuleInfoFromElement).value();

            $(element).prop("disabled", true);

            performDeleteModules(this.data.realmPath, moduleInfos).then(() => {
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
                chainsPromise,
                modulesPromise;

            this.data.args = args;
            this.data.realmPath = args[0];

            chainsPromise = AuthenticationService.authentication.chains.all(this.data.realmPath);
            modulesPromise = AuthenticationService.authentication.modules.all(this.data.realmPath);

            Promise.all([chainsPromise, modulesPromise]).then((values) => {
                _.each(values[1][0].result, (module) => {
                    _.each(values[0].values.result, (chain) => {
                        _.each(chain.authChainConfiguration, (link) => {
                            if (link.module === module._id) {
                                module.chains = module.chains || [];
                                module.chains.push(chain._id);
                            }
                        });
                    });
                    module.chains = _.uniq(module.chains);
                });

                self.data.formData = values[1][0].result;

                self.parentRender(() => {
                    if (callback) {
                        callback();
                    }
                });
            }, (errorChains, errorModules) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: errorChains ? errorChains : errorModules
                });

                self.parentRender(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        save (event) {
            var promise = AuthenticationService.authentication.update(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.currentTarget);
        }
    });

    return ModulesView;
});
