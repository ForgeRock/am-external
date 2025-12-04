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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { t } from "i18next";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import EditSiteTemplate from "templates/admin/views/deployment/sites/EditSiteTemplate";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SitesService from "org/forgerock/openam/ui/admin/services/global/SitesService";

function toggleSave (el, enable) {
    el.find("[data-save]").prop("disabled", !enable);
}

const EditSitesView = AbstractView.extend({
    template: EditSiteTemplate,
    events: {
        "click [data-save]": "onSave",
        "click [data-delete]": "onDelete"
    },

    render (args) {
        this.data.id = args[0];
        SitesService.sites.get(this.data.id).then((data) => {
            this.data.name = data.values.raw._id;
            this.data.etag = data.values.raw.etag;
            this.data.headerActions = [{
                actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times"
            }];

            this.parentRender(() => {
                new Backlink().render();
                if (this.jsonSchemaView) {
                    this.jsonSchemaView.remove();
                }
                this.jsonSchemaView = new FlatJSONSchemaView({
                    schema: data.schema,
                    values: data.values,
                    onRendered: () => toggleSave(this.$el, true)
                });
                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
            });
        });
    },

    onSave () {
        SitesService.sites.update(this.data.id, this.jsonSchemaView.getData(), this.data.etag)
            .then(
                () => Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") }),
                (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER })
            );
    },

    onDelete (event) {
        event.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteText", { type: t("console.sites.common.confirmType") })
        }, () => {
            SitesService.sites.remove(this.data.id, this.data.etag).then(
                () => Router.routeTo(Router.configuration.routes.listSites, { args: [], trigger: true }),
                (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER })
            );
        });
    }
});

export default new EditSitesView();
