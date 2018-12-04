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
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServicesService from "org/forgerock/openam/ui/admin/services/realm/ServicesService";
import ServicesTemplate from "templates/admin/views/realms/services/ServicesTemplate";

function getServiceIdFromElement (element) {
    return $(element).closest("tr").data("serviceId");
}
function deleteServices (ids) {
    FormHelper.showConfirmationBeforeDeleting({
        message: $.t("console.services.list.confirmDeleteSelected", { count: ids.length })
    }, () => {
        ServicesService.instance.remove(this.data.realmPath, ids).then(() => {
            this.rerender();
        }, (response) => {
            Messages.addMessage({ type: Messages.TYPE_DANGER, response });
            this.rerender();
        });
    });
}
const ServicesView = AbstractView.extend({
    template: ServicesTemplate,
    events: {
        "change [data-select-service]": "serviceSelected",
        "click [data-delete-service]":  "onDeleteSingle",
        "click [data-delete-services]": "onDeleteMultiple",
        "click [data-add-service]":     "onAddService"
    },
    serviceSelected (event) {
        const anyServicesSelected = this.$el.find("input[type=checkbox]").is(":checked");
        const row = $(event.currentTarget).closest("tr");

        row.toggleClass("selected");
        this.$el.find("[data-delete-services]").prop("disabled", !anyServicesSelected);
    },
    onAddService (event) {
        event.preventDefault();
        Router.routeTo(Router.configuration.routes.realmsServiceNew, {
            args: [encodeURIComponent(this.data.realmPath)],
            trigger: true
        });
    },
    onDeleteSingle (event) {
        event.preventDefault();
        const id = getServiceIdFromElement(event.currentTarget);
        _.bind(deleteServices, this)([id]);
    },
    onDeleteMultiple (event) {
        event.preventDefault();

        const ids = _(this.$el.find("input[type=checkbox]:checked")).toArray().map(getServiceIdFromElement).value();

        _.bind(deleteServices, this)(ids);
    },
    validateAddButton (creatables) {
        if (!_.isEmpty(creatables)) {
            return;
        }
        this.$el.find("[data-add-service]").addClass("disabled").popover({
            trigger : "hover",
            container : "body",
            placement : "top",
            content: $.t("console.services.edit.unavaliable")
        });
    },
    render (args, callback) {
        this.data.args = args;
        this.data.realmPath = args[0];

        Promise.all([
            ServicesService.instance.getAll(this.data.realmPath),
            ServicesService.type.getCreatables(this.data.realmPath)
        ]).then(([services, creatables]) => {
            this.data.services = _.sortBy(services.result, "name");
            const sortedCreatables = _.sortBy(creatables.result, "name");

            this.parentRender(() => {
                this.validateAddButton(sortedCreatables);
                if (callback) {
                    callback();
                }
            });
        });
    },
    rerender () {
        this.render(this.data.args);
    }
});

export default ServicesView;
