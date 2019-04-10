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
/* eslint-disable max-len */

import { upperFirst } from "lodash";

const GlobalRoutes = {
    listAuthenticationSettings: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/authentication/ListAuthenticationView.jsx"),
        url: /configure\/authentication$/,
        pattern: "configure/authentication",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editAuthenticationSettings: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/authentication/EditGlobalAuthenticationView.js"),
        url: /configure\/authentication\/([^/]+)/,
        pattern: "configure/authentication/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    listGlobalServices: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/global/ListGlobalServicesView.jsx"),
        url: /configure\/globalServices$/,
        pattern: "configure/globalServices",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editGlobalService: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceView.js"),
        url: /configure\/globalServices\/([^/]+)$/,
        pattern: "configure/globalServices/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    globalServiceSubSchemaNew: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/global/NewGlobalServiceSubSchemaView.js"),
        url: /configure\/globalServices\/([^/]+)\/([^/]+)\/new/,
        pattern: "configure/globalServices/?/?/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    globalServiceSubSchemaEdit: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceSubSchemaView.js"),
        url: /configure\/globalServices\/([^/]+)\/([^/]+)\/edit\/([^/]+)/,
        pattern: "configure/globalServices/?/?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    globalServiceSubSubSchemaEdit: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceSubSubSchemaView.js"),
        url: /configure\/globalServices\/([^/]+)\/([^/]+)\/edit\/([^/]+)\/([^/]+)\/edit\/([^/]+)/,
        pattern: "configure/globalServices/?/?/edit/?/?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    listGlobalSecretStores: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/secretStores/list/ListGlobalSecretStoresContainer.jsx"),
        url: /configure\/secretStores/,
        pattern: "configure/secretStores",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    newGlobalSecretStores: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/secretStores/new/NewGlobalSecretStoresContainer.jsx"),
        url: /configure\/secretStores\/new$/,
        pattern: "configure/secretStores/new",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editGlobalSecretStores: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/secretStores/edit/EditGlobalSecretStoreContainer.jsx"),
        url: /configure\/secretStores\/([^/]+)\/edit\/([^/]+)/,
        pattern: "configure/secretStores/?/edit/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editGlobalSingletonSecretStores: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/secretStores/edit/EditGlobalSingletonSecretStoreContainer.jsx"),
        url: /configure\/secretStores\/persistent\/([^/]+)\/edit/,
        pattern: "configure/secretStores/persistent/?/edit",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editSecretStoresCoreAttributes: {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/secretStores/core/EditGlobalSecretStoresView.js"),
        url: /configure\/secretStores\/core/,
        pattern: "configure/secretStores/core",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    listSites: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/sites/ListSitesView.js"),
        url: /deployment\/sites$/,
        pattern: "deployment/sites",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editSite: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/sites/EditSiteView.js"),
        url: /deployment\/sites\/edit\/([^/]+)/,
        pattern: "deployment/sites/edit/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    newSite: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/sites/NewSiteView.js"),
        url: /deployment\/sites\/new/,
        pattern: "deployment/sites/new",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    listServers: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/servers/ListServersView.js"),
        url: /deployment\/servers$/,
        pattern: "deployment/servers",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    newServer: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/servers/NewServerView.js"),
        url: /deployment\/servers\/new$/,
        pattern: "deployment/servers/new",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    cloneServer: {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/servers/NewServerView.js"),
        url: /deployment\/servers\/clone\/([^/]+)/,
        pattern: "deployment/servers/clone/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    apiExplorer: {
        view: () => import("org/forgerock/openam/ui/admin/views/api/ListApiView.jsx"),
        role: "ui-global-admin",
        url: /^api\/explorer\/(.*)/,
        pattern: "api/explorer/?",
        navGroup: "admin"
    },
    apiDoc: {
        view: () => import("org/forgerock/openam/ui/admin/views/api/ApiDocView.js"),
        role: "ui-global-admin",
        url: /^api\/doc/,
        pattern: "api/doc",
        navGroup: "admin"
    }
};

// Add routes for "Server Edit" tree navigation
["general", "security", "session", "sdk", "cts", "uma", "advanced", "directoryConfiguration"].forEach((suffix) => {
    GlobalRoutes[`editServer${upperFirst(suffix)}`] = {
        view: () => import("org/forgerock/openam/ui/admin/views/deployment/servers/EditServerTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/common/server/EditServerView.js"),
        url: new RegExp(`deployment/servers/([^/]+)/(${suffix})`),
        pattern: `deployment/servers/?/${suffix}`,
        role: "ui-global-admin",
        navGroup: "admin",
        forceUpdate: true
    };
});

// Add routes for "Server Defaults" tree navigation
["general", "security", "session", "sdk", "cts", "uma", "advanced"].forEach((suffix) => {
    GlobalRoutes[`editServerDefaults${upperFirst(suffix)}`] = {
        view: () => import("org/forgerock/openam/ui/admin/views/configuration/server/EditServerDefaultsTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/common/server/EditServerView.js"),
        url: new RegExp(`configure/(serverDefaults)/(${suffix})`),
        pattern: `configure/serverDefaults/${suffix}`,
        role: "ui-global-admin",
        navGroup: "admin",
        forceUpdate: true
    };
});

export default GlobalRoutes;
