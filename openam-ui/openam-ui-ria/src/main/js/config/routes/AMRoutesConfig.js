/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () => {
    return {
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
        switchRealm: {
            view: () => import("org/forgerock/openam/ui/user/login/SwitchRealmView.js"),
            role: "ui-user",
            url: /switchRealm(\/[^&]*)(&.+)?/,
            pattern: "switchRealm?",
            defaults: [""],
            argumentNames: ["additionalParameters"]
        },
        dashboard: {
            view: () => import("org/forgerock/openam/ui/user/dashboard/views/DashboardView.jsm"),
            role: "ui-self-service-user",
            url: "dashboard/",
            forceUpdate: true,
            navGroup: "user"
        },
        loggedOut: {
            view: () => import("org/forgerock/openam/ui/user/login/RESTLogoutView.js"),
            url: /loggedOut([^&]+)?(&.+)?/,
            pattern: "loggedOut??",
            defaults: ["/", ""],
            argumentNames: ["realm", "additionalParameters"]
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
});
