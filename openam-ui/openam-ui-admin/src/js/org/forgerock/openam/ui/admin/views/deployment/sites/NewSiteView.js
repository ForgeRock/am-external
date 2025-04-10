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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewSiteTemplate from "templates/admin/views/deployment/sites/NewSiteTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import SitesService from "org/forgerock/openam/ui/admin/services/global/SitesService";

const NewSiteView = AbstractView.extend({
    template: NewSiteTemplate,
    events: {
        "click [data-create]": "onCreate",
        "keyup  [data-site-name]": "onValidateProps"
    },

    render () {
        SitesService.sites.getInitialState().then((data) => this.parentRender(() => {
            new Backlink().render();
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: data.schema,
                values: data.values
            });
            $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-site-form]"));
        }));
    },

    onCreate () {
        const values = _.cloneDeep(this.jsonSchemaView.getData());
        const siteId = this.$el.find("[data-site-name]").val();
        values["_id"] = siteId;

        // This doesn't have the values.removeNullPasswords fix from OPENAM-11834 as not required for this view.
        // However it might need adding in the future.
        SitesService.sites.create(values)
            .then(() => { Router.routeTo(Router.configuration.routes.listSites, { args: [], trigger: true }); },
                (response) => { Messages.addMessage({ response, type: Messages.TYPE_DANGER }); }
            );
    },

    onValidateProps (event) {
        let siteId = $(event.currentTarget).val();
        if (siteId.indexOf(" ") !== -1) {
            siteId = false;
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.sites.new.nameValidationError")
            });
        }
        this.$el.find("[data-create]").prop("disabled", !siteId);
    }
});

export default new NewSiteView();
