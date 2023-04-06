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
 * Copyright 2015-2022 ForgeRock AS.
 */

import "jquery-sortable";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";
import Handlebars from "handlebars-template-loader/runtime";

import { show as showDeleteDialog } from "components/dialogs/Delete";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AlertPartial from "partials/alerts/_Alert";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import EditChainAlertPartial from "templates/admin/views/realms/authentication/chains/_EditChainAlertPartial";
import EditChainTemplate from "templates/admin/views/realms/authentication/chains/EditChainTemplate";
import EditLinkView from "org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditLinkView";
import LinkView from "org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PostProcessView from "org/forgerock/openam/ui/admin/views/realms/authentication/chains/PostProcessView";
import Router from "org/forgerock/commons/ui/common/main/Router";
import {
    isPlaceholder
} from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

Handlebars.registerHelper("isPlaceholder", function (str, options) {
    if (isPlaceholder(str)) {
        return options.fn(this);
    }
    return options.inverse(this);
});

const createLinkView = function (index, view) {
    const linkView = new LinkView();

    /**
          * A new list item is being dynamically created and added to the current EditChainView as a child View.
          * In order to do this we must create the element here, parent and pass it to the child so that it has
          * something to render inside of.
          */
    linkView.el = $("<li class='chain-link' />");
    linkView.element = linkView.el;
    linkView.parent = view;

    linkView.data = {
        // Each linkview instance requires allCriteria and allModules to render. These values are never changed
        // Because multiple instances require this same data, I grab it only in this parent view, then pass it
        // on to to all the child linkview instances.
        typeDescription: "",
        allModules: view.data.allModules,
        linkConfig: view.data.form.chainData.authChainConfiguration[index],
        allCriteria: {
            REQUIRED: t("console.authentication.editChains.criteria.0.title"),
            OPTIONAL: t("console.authentication.editChains.criteria.1.title"),
            REQUISITE: t("console.authentication.editChains.criteria.2.title"),
            SUFFICIENT: t("console.authentication.editChains.criteria.3.title")
        }
    };

    return linkView;
};

const initSortable = function (self) {
    self.$el.find("ol#sortableAuthChain").sortable({
        exclude: "li:not(.chain-link)",
        delay: 100,
        vertical: true,
        placeholder: "<li class='placeholder'><div class='placeholder-inner'></div></i>",

        onDrag (item, position) {
            item.css({
                left: position.left - self.adjustment.left,
                top: position.top - self.adjustment.top
            });
        },

        onDragStart (item, container) {
            const offset = item.offset();
            const pointer = container.rootGroup.pointer;

            self.adjustment = {
                left: pointer.left - offset.left + 5,
                top: pointer.top - offset.top
            };
            self.originalIndex = item.index();
            item.addClass("dragged");
            item.width(item.width());
            $("body").addClass("dragging");
        },

        onDrop (item, container, _super) {
            self.sortChainData(self.originalIndex, item.index());
            self.validateChain();
            _super(item, container);
        }
    });
};

export default AbstractView.extend({
    template: EditChainTemplate,

    events: {
        "click [data-save-chain]": "saveChain",
        "click [data-save-settings]": "saveSettings",
        "click [data-add-new-module]": "addNewModule",
        "click [data-delete]": "onDeleteClick"
    },

    partials: {
        "alerts/_Alert": AlertPartial
    },

    addItemToList (element) {
        this.$el.find("ol#sortableAuthChain").append(element);
    },

    addNewModule () {
        const index = this.data.form.chainData.authChainConfiguration.length;
        const linkView = createLinkView(index, this);
        this.editItem(linkView);
    },

    editItem (linkview) {
        EditLinkView.show(linkview);
    },

    onDeleteClick (event) {
        event.preventDefault();
        if ($(event.currentTarget).hasClass("disabled")) { return false; }

        const id = this.data.form.chainData._id;

        showDeleteDialog({
            names: [id],
            objectName: "chain",
            onConfirm: async () => {
                await AuthenticationService.authentication.chains.remove(this.data.realmPath, [id]);
                Messages.addMessage({
                    type: Messages.TYPE_INFO,
                    message: t("console.authentication.editChains.deletedChain")
                });
                Router.routeTo(Router.configuration.routes.realmsAuthenticationChains, {
                    args: [encodeURIComponent(this.data.realmPath)],
                    trigger: true
                });
            }
        });
    },

    render (args) {
        const self = this;

        AuthenticationService.authentication.chains.get(args[0], args[1]).then((data) => {
            // EditChainsTemplate expects form values to be an array,
            // placeholders are not in that format so make them an array
            const chainData = data.chainData;
            Object.keys(chainData).forEach((key) => {
                if (isPlaceholder(chainData[key])) {
                    chainData[key] = [chainData[key]];
                }
            });

            self.data = {
                realmPath: args[0],
                allModules: data.modulesData,
                form: { chainData },
                headerActions: [
                    { actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }
                ]
            };

            self.parentRender(() => {
                if (self.data.form.chainData.adminAuthModule || self.data.form.chainData.orgConfig) {
                    const popoverOpt = {
                        trigger: "hover",
                        container: "body",
                        placement: "top"
                    };

                    if (self.data.form.chainData.adminAuthModule && self.data.form.chainData.orgConfig) {
                        popoverOpt.content =
                            t("console.authentication.editChains.deleteBtnTooltip.defaultAdminOrgAuthChain");
                    } else if (self.data.form.chainData.adminAuthModule) {
                        popoverOpt.content =
                            t("console.authentication.editChains.deleteBtnTooltip.defaultAdminAuthChain");
                    } else {
                        popoverOpt.content =
                            t("console.authentication.editChains.deleteBtnTooltip.defaultOrgAuthChain");
                    }
                    // popover doesn't work in case button has disabled attribute
                    self.$el.find("[data-delete]").addClass("disabled").popover(popoverOpt);
                }

                if (self.data.form.chainData.authChainConfiguration.length > 0) {
                    _.each(self.data.form.chainData.authChainConfiguration, (linkConfig, index) => {
                        const linkView = createLinkView(index, self);
                        self.addItemToList(linkView.element);
                        linkView.render();
                    });
                } else {
                    self.validateChain();
                }

                initSortable(self);
                PostProcessView.render(self.data.form.chainData);
            });
        }, (response) => {
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                response
            });
        });
    },

    saveSettings () {
        const self = this;
        const chainData = this.data.form.chainData;

        chainData.loginSuccessUrl = this.$el.find("#loginSuccessUrl").val();
        chainData.loginFailureUrl = this.$el.find("#loginFailureUrl").val();

        PostProcessView.addClassNameDialog().then(() => {
            const savedData = {
                loginFailureUrl: chainData.loginFailureUrl,
                loginPostProcessClass: chainData.loginPostProcessClass,
                loginSuccessUrl: chainData.loginSuccessUrl
            };

            AuthenticationService.authentication.chains.update(self.data.realmPath, chainData._id, savedData)
                .then(() => {
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                }, (response) => {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response
                    });
                });
        });
    },

    saveChain () {
        const chainData = this.data.form.chainData;
        const savedData = {
            authChainConfiguration: chainData.authChainConfiguration
        };

        AuthenticationService.authentication.chains.update(this.data.realmPath, chainData._id, savedData)
            .then(() => {
                Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
    },

    sortChainData (from, to) {
        const addItem = this.data.form.chainData.authChainConfiguration.splice(from, 1)[0];
        this.data.form.chainData.authChainConfiguration.splice(to, 0, addItem);
    },

    validateChain () {
        let invalid = false;
        let alert = "";
        let firstRequiredIndex;
        let sufficentIndex;
        const config = this.data.form.chainData.authChainConfiguration;

        if (config.length === 0) {
            invalid = true;
            this.$el.find("#sortableAuthChain").addClass("hidden");
            this.$el.find("#lowerAuthChainsLegend").addClass("hidden");
            this.$el.find(".btn-toolbar").addClass("hidden");
            this.$el.find(".call-to-action-block").removeClass("hidden");
        } else {
            this.$el.find(".call-to-action-block").addClass("hidden");
            firstRequiredIndex = _.findIndex(config, { criteria: "REQUIRED" });
            sufficentIndex = _.findIndex(_.drop(config, firstRequiredIndex), { criteria: "SUFFICIENT" });

            if (firstRequiredIndex > -1 && sufficentIndex > -1 &&
                firstRequiredIndex < sufficentIndex &&
                config.length - 1 > sufficentIndex) {
                alert = EditChainAlertPartial();
            }

            this.$el.find("#sortableAuthChain").removeClass("hidden");
            this.$el.find("#lowerAuthChainsLegend").removeClass("hidden");
            this.$el.find(".btn-toolbar").removeClass("hidden");
        }

        this.$el.find("#alertContainer").html(alert);
        this.$el.find("[data-save-chain]").prop("disabled", invalid);
    }
});
