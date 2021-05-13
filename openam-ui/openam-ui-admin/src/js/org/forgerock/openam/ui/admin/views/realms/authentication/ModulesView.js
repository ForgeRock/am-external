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

import "selectize";

import { each, uniq } from "lodash";
import $ from "jquery";

import { show as showDeleteDialog } from "components/dialogs/Delete";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AlertPartial from "partials/alerts/_Alert";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import EditModuleDialog from "org/forgerock/openam/ui/admin/views/realms/authentication/EditModuleDialog";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import ModulesTemplate from "templates/admin/views/realms/authentication/ModulesTemplate";

const getModuleInfoFromElement = (element) => $(element).closest("tr").data();

const ModulesView = AbstractView.extend({
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
        const hasModuleSelected = this.$el.find("input[type=checkbox]").is(":checked");
        const row = $(event.currentTarget).closest("tr");
        const checked = $(event.currentTarget).is(":checked");

        this.$el.find("[data-delete-modules]").prop("disabled", !hasModuleSelected);
        if (checked) {
            row.addClass("selected");
        } else {
            row.removeClass("selected");
        }
    },
    editModule (event) {
        event.preventDefault();
        const data = $(event.currentTarget).closest("tr").data();
        const href = event.currentTarget.href;

        EditModuleDialog(data.moduleName, data.moduleChains, href);
    },
    onDeleteSingle (event) {
        event.preventDefault();

        const moduleInfos = [getModuleInfoFromElement(event.currentTarget)];

        this.deleteModules(moduleInfos);
    },
    onDeleteMultiple (event) {
        event.preventDefault();

        const moduleInfos = this.$el
            .find("input[type=checkbox]:checked")
            .map((index, element) => getModuleInfoFromElement(element))
            .toArray();

        this.deleteModules(moduleInfos);
    },
    deleteModules (moduleInfos) {
        showDeleteDialog({
            names: moduleInfos.map((moduleInfo) => moduleInfo.moduleName),
            objectName: "module",
            onConfirm: async () => {
                const promises = moduleInfos.map(({ moduleName, moduleType }) =>
                    AuthenticationService.authentication.modules.remove(this.data.realmPath, moduleName, moduleType));
                await Promise.all(promises);

                this.render([this.data.realmPath]);
            }
        });
    },
    render (args, callback) {
        const self = this;

        this.data.args = args;
        this.data.realmPath = args[0];

        const chainsPromise = AuthenticationService.authentication.chains.all(this.data.realmPath);
        const modulesPromise = AuthenticationService.authentication.modules.all(this.data.realmPath);

        Promise.all([chainsPromise, modulesPromise]).then(([chains, modules]) => {
            each(modules, (module) => {
                each(chains.values.result, (chain) => {
                    each(chain.authChainConfiguration, (link) => {
                        if (link.module === module._id) {
                            module.chains = module.chains || [];
                            module.chains.push(chain._id);
                        }
                    });
                });
                module.chains = uniq(module.chains);
            });

            self.data.formData = modules;

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
    }
});

export default ModulesView;
