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
 * Copyright 2014-2025 Ping Identity Corporation.
 */

import "autosizeInput";
import "doTimeout";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import CreatedResourcesTemplate from "templates/admin/views/realms/authorization/policies/CreatedResourcesTemplate";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PopulateResourceTemplate from "templates/admin/views/realms/authorization/policies/PopulateResourceTemplate";

const CreatedResourcesView = AbstractView.extend({
    element: "#editResources",
    template: CreatedResourcesTemplate,
    noBaseTemplate: true,
    events: {
        "click [data-add-resource]": "addResource",
        "keyup [data-add-resource]": "addResource",
        "keyup #resourceBuilder input": "addResource",
        "click span[data-delete]": "deleteResource",
        "keyup span[data-delete]": "deleteResource",
        "click [data-remove-pending]": "removePendingResource",
        "click [data-show-editing]" : "showEditingResources"
    },

    render (args, callback) {
        _.extend(this.data, args);

        if (this.data.entity.resources) {
            this.data.entity.resources = _.sortBy(this.data.entity.resources);
        } else {
            this.data.entity.resources = [];
        }

        const self = this;

        this.parentRender(() => {
            this.$el.find(".selectize").selectize({
                sortField: {
                    field: "text",
                    direction: "asc"
                },
                onChange (value) {
                    self.data.options.newPattern = value;

                    const content = PopulateResourceTemplate(self.data);
                    const resources = self.$el.find("#populateResource");
                    resources.html(content);

                    resources.find("input").autosizeInput({ space: 19 });
                    resources.find("input:eq(0)").focus().select();
                }
            });

            delete self.data.options.justAdded;
            self.flashDomItem(self.$el.find(".text-success"), "text-success");

            if (callback) { callback(); }
        });
    },

    validate (inputs) {
        // This is very simple native validation for supporting browsers for now.
        // More complexity to come later.
        let allInputsValid = true;

        _.find(inputs, (input) => {
            if (_.isFunction(input.checkValidity) && !input.checkValidity()) {
                allInputsValid = false;
                return;
            }
        });

        return allInputsValid;
    },

    addResource (e) {
        if (e.type === "keyup" && e.keyCode !== 13) {
            return;
        }

        const resourceBuilder = this.$el.find("#resourceBuilder");
        const resourceStr = resourceBuilder.data().resource.replace("-*-", "̂");
        const inputs = resourceBuilder.find("input");
        const strLength = resourceStr.length;
        let resource = "";
        let count = 0;
        let i;

        if (this.validate(inputs) === false) {
            Messages.addMessage({ message: t("console.authorization.common.invalidItem"), type: Messages.TYPE_DANGER });
            this.flashDomItem(this.$el.find(".editing"), "text-danger");
            return;
        }

        for (i = 0; i < strLength; i++) {
            if (resourceStr[i] === "*") {
                resource += inputs[count].value;
                count++;
            } else if (resourceStr[i] === "̂") {
                resource += inputs[count].value === "̂" ? "-*-" : inputs[count].value;
                count++;
            } else {
                resource += resourceStr[i];
            }
        }

        const duplicateIndex = _.indexOf(this.data.entity.resources, resource);

        if (duplicateIndex >= 0) {
            Messages.addMessage({
                message: t("console.authorization.common.duplicateItem"),
                type: Messages.TYPE_DANGER
            });
            this.flashDomItem(this.$el.find(`#createdResources ul li:eq(${duplicateIndex})`), "text-danger");
        } else {
            this.data.entity.resources.push(resource);
            this.data.options.justAdded = resource;
            this.render(this.data);
        }
    },

    removePendingResource (e) {
        if (e.type === "keyup" && e.keyCode !== 13) {
            return;
        }

        this.data.options.newPattern = null;
        this.render(this.data);
    },

    deleteResource (e) {
        if (e.type === "keyup" && e.keyCode !== 13) {
            return;
        }
        const resource = $(e.currentTarget).parent().data().resource;
        this.data.entity.resources = _.without(this.data.entity.resources, resource);
        this.render(this.data);
    },

    showEditingResources () {
        this.$el.find("[data-show-editing]").prop("disabled", true);
        this.$el.find("li.editing").removeClass("hidden");
    },

    flashDomItem (item, className) {
        item.addClass(className);
        $.doTimeout(_.uniqueId(className), 2000, () => {
            item.removeClass(className);
        });
    }
});

export default new CreatedResourcesView();
