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
 * Copyright 2018 ForgeRock AS.
 */

export default {
    continuePasswordReset: {
        view: () => import("org/forgerock/openam/ui/user/anonymousProcess/PasswordResetView.js"),
        url: /continuePasswordReset(\/[^&]*)(&.+)?/,
        pattern: "continuePasswordReset??",
        forceUpdate: true,
        defaults: ["/", ""],
        argumentNames: ["realm", "additionalParameters"]
    },
    continueSelfRegister: {
        view: () => import("org/forgerock/openam/ui/user/anonymousProcess/SelfRegistrationView.js"),
        url: /continueRegister(\/[^&]*)(&.+)?/,
        pattern: "continueRegister??",
        forceUpdate: true,
        defaults: ["/", ""],
        argumentNames: ["realm", "additionalParameters"]
    },
    "forgotUsername": {
        view: () => import("ForgotUsernameView"),
        url: /^forgotUsername(\/[^&]*)(&.+)?/,
        pattern: "forgotUsername??",
        argumentNames: ["realm", "additionalParameters"],
        defaults: ["/", ""]
    },
    "passwordReset": {
        view: () => import("PasswordResetView"),
        url: /^passwordReset(\/[^&]*)(&.+)?/,
        pattern: "passwordReset??",
        argumentNames: ["realm", "additionalParameters"],
        defaults: ["/", ""]
    },
    "selfRegistration": {
        view: () => import("RegisterView"),
        url: /^register(\/[^&]*)(&.+)?/,
        pattern: "register??",
        argumentNames: ["realm", "additionalParameters"],
        defaults: ["/", ""]
    }
};
