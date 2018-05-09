/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/dashboard/services/OAuthTokensService"
], ($, _, AbstractView, OAuthTokensService) => {
    var OAuthToken = AbstractView.extend({
        template: "user/dashboard/TokensTemplate",
        noBaseTemplate: true,
        element: "#myOAuthTokensSection",
        events: {
            "click a.deleteToken": "deleteToken"
        },

        render () {
            var self = this;

            OAuthTokensService.getApplications().then((data) => {
                self.data.applications = _.map(data.result, (application) => {
                    return {
                        id: application._id,
                        name: application.name,
                        scopes: _.values(application.scopes).join(", "),
                        expiryDateTime: application.expiryDateTime
                            ? new Date(application.expiryDateTime).toLocaleString()
                            : $.t("openam.oAuth2.tokens.neverExpires")
                    };
                });
                self.parentRender(() => {
                    self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
                });
            });
        },

        deleteToken (event) {
            event.preventDefault();
            var self = this;

            OAuthTokensService.revokeApplication(event.currentTarget.id).then(() => {
                self.render();
            }, () => {
                console.error("Failed to revoke application");
            });
        }
    });

    return new OAuthToken();
});
