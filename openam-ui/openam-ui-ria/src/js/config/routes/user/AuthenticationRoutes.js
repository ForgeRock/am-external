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
 * Copyright 2018-2019 ForgeRock AS.
 */

export default {
    login: {
        view: () => import("LoginView"),
        url: /^login([^&]+)?(&.+)?/,
        pattern: "login??",
        defaults: ["/", ""],
        argumentNames: ["realm", "additionalParameters"]
    },
    logout: {
        view: () => import("org/forgerock/openam/ui/user/login/RESTLogoutView.js"),
        url: /^logout(.*)/,
        pattern: "logout?",
        defaults: [""],
        argumentNames: ["additionalParameters"]
    },
    loginFailure: {
        view: () => import("org/forgerock/openam/ui/user/login/LoginFailureView.js"),
        url: /failedLogin([^&]+)?(&.+)?/,
        pattern: "failedLogin??",
        defaults: ["/", ""],
        argumentNames: ["realm", "additionalParameters"]
    },
    sessionExpired: {
        view: () => import("org/forgerock/openam/ui/user/login/SessionExpiredView.js"),
        url: /sessionExpired([^&]+)?(&.+)?/,
        pattern: "sessionExpired??",
        defaults: ["/", ""],
        argumentNames: ["realm", "additionalParameters"]
    }
};
