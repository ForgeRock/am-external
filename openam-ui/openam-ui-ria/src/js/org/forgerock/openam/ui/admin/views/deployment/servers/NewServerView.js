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

import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Backlink from "org/forgerock/openam/ui/admin/views/common/Backlink";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewServerTemplate from "templates/admin/views/deployment/servers/NewServerTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServersService from "org/forgerock/openam/ui/admin/services/global/ServersService";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const getTrimmedValue = (field) => field.val().trim();
const sendErrorMessage = (response) => {
    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
};
const routeToEdit = (id) => {
    Router.routeTo(Router.configuration.routes.editServerGeneral, {
        args: [id],
        trigger: true
    });
};

const NewServerView = AbstractView.extend({
    template: NewServerTemplate,
    events: {
        "click [data-create]": "createServer",
        "keyup [data-server-url]": "toggleCreateButton"
    },
    render ([id]) {
        this.data.id = id;
        const fragments = URIUtils.getCurrentFragment().split("/");
        this.isCloneView = fragments.indexOf("clone") !== -1;
        if (this.isCloneView) {
            this.data.title = "console.servers.clone.title";
            this.data.buttonTitle = "common.form.clone";
        } else {
            this.data.title = "console.servers.new.title";
            this.data.buttonTitle = "common.form.create";
        }
        this.parentRender(() => { new Backlink().render(); });
        return this;
    },
    createServer () {
        const serverUrl = getTrimmedValue(this.$el.find("[data-server-url]"));

        if (this.isCloneView) {
            ServersService.servers.clone(this.data.id, serverUrl).then((response) => {
                routeToEdit(response.clonedId);
            }, sendErrorMessage);
        } else {
            ServersService.servers.create({ "url": serverUrl }).then((response) => {
                routeToEdit(response._id);
            }, sendErrorMessage);
        }
    },
    toggleCreateButton (event) {
        const serverUrl = getTrimmedValue($(event.currentTarget));
        const valid = serverUrl !== "";

        this.$el.find("[data-create]").prop("disabled", !valid);
    }
});

export default new NewServerView();
