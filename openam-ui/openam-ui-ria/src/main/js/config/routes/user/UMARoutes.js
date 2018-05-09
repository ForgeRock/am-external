/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define(() => {
    return {
        // My Resources
        "umaResourcesMyResources": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/MyResourcesPage.js"),
            url: /^uma\/resources\/?(myresources)?\/?([^/]+)?\/?$/,
            pattern: "uma/resources/?/?",
            role: "ui-uma-user",
            navGroup: "user",
            defaults: ["myresources", ""],
            forceUpdate: true
        },
        "umaResourcesMyResourcesResource": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/ResourcePage.js"),
            url: /^uma\/resources\/myresources\/([^/]+)\/([^/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/myresources/?/?",
            defaults: [""],
            forceUpdate: true
        },

        // Shared with me
        "umaResourcesSharedWithMe": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/SharedWithMePage.js"),
            url: /^uma\/resources\/sharedwithme\/?$/,
            pattern: "uma/resources/sharedwithme",
            role: "ui-uma-user",
            navGroup: "user",
            forceUpdate: true
        },
        "umaResourcesSharedWithMeResource": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/ResourcePage.js"),
            url: /^uma\/resources\/sharedwithme\/([^/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/sharedwithme/?",
            defaults: [""],
            forceUpdate: true
        },

        // Starred
        "umaResourcesStarred": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/StarredPage.js"),
            url: /^uma\/resources\/starred\/?$/,
            pattern: "uma/resources/starred",
            role: "ui-uma-user",
            navGroup: "user",
            forceUpdate: true
        },
        "umaResourcesStarredResource": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/ResourcePage.js"),
            url: /^uma\/resources\/starred\/([^/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/starred/?",
            defaults: [""],
            forceUpdate: true
        },

        // My Labels
        "umaResourcesMyLabels": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/MyLabelsPage.js"),
            url: /^uma\/resources\/mylabels\/([^/]+)\/?$/,
            pattern: "uma/resources/mylabels/?",
            role: "ui-uma-user",
            navGroup: "user",
            defaults: [""],
            forceUpdate: true
        },
        "umaResourcesMyLabelsResource": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView.js"),
            page: () => import("org/forgerock/openam/ui/user/uma/views/resource/ResourcePage.js"),
            url: /^uma\/resources\/mylabels\/([^/]+)\/([^/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/mylabels/?/?",
            defaults: ["", ""],
            forceUpdate: true
        },

        // History
        "umaHistory": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/history/ListHistory.js"),
            role: "ui-uma-user",
            navGroup: "user",
            url: /^uma\/history\/?$/,
            pattern: "uma/history"
        },
        // Requests
        "umaRequestEdit": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/request/EditRequest.js"),
            role: "ui-uma-user",
            navGroup: "user",
            url: /^uma\/requests\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/requests/?"
        },
        "umaRequestList": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/request/ListRequest.js"),
            role: "ui-uma-user",
            navGroup: "user",
            defaults: [""],
            url: /^uma\/requests\/?$/,
            pattern: "uma/requests/"
        },
        // Share
        "umaBaseShare": {
            view: () => import("org/forgerock/openam/ui/user/uma/views/share/BaseShare.js"),
            url: /^uma\/share\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/share/?",
            defaults: [""],
            role: "ui-uma-user",
            navGroup: "user"
        }
    };
});
