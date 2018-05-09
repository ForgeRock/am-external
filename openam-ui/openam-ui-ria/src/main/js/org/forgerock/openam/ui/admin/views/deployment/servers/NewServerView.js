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
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/admin/services/global/ServersService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "templates/admin/views/deployment/servers/NewServerTemplate"
], ($, _, Messages, AbstractView, Router, URIUtils, ServersService, Backlink, NewServerTemplate) => {
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

    return new NewServerView();
});
