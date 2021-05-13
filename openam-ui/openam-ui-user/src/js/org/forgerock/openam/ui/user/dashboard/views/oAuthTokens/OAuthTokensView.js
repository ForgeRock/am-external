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
 * Copyright 2015-2019 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import OAuthTokensService from "org/forgerock/openam/ui/user/dashboard/services/OAuthTokensService";

const OAuthToken = AbstractView.extend({
    template: "user/dashboard/oAuthTokens/TokensTemplate",
    noBaseTemplate: true,
    element: "#myOAuthTokensSection",
    events: {
        "click a.deleteToken": "deleteToken"
    },

    render () {
        const self = this;

        OAuthTokensService.getApplications().then((data) => {
            self.data.applications = _.map(data.result, (application) => {
                return {
                    id: application._id,
                    name: application.name || application._id,
                    logoUri: application.logoUri,
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
        const self = this;

        OAuthTokensService.revokeApplication(event.currentTarget.id).then(() => {
            self.render();
        }, () => {
            console.error("Failed to revoke application");
        });
    }
});

export default new OAuthToken();
