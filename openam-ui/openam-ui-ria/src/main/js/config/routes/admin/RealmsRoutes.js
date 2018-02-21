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
 * Copyright 2015-2017 ForgeRock AS.
 */

define(() => {
    const scopedByRealm = (fragment) => new RegExp(`^realms/((?:%2F)[^/]*)/${fragment}$`);
    const defaultScopedByRealm = (fragment) => scopedByRealm(`?(?:${fragment})?`);
    const viewPrefix = "org/forgerock/openam/ui/admin/views/realms/";
    const routes = {
        "realms": {
            view: `${viewPrefix}ListRealmsView`,
            url: /^realms\/*$/,
            pattern: "realms",
            role: "ui-realm-admin",
            navGroup: "admin"
        },
        "realmEdit": {
            view: `${viewPrefix}EditRealmView`,
            url: scopedByRealm("edit"),
            pattern: "realms/?/edit",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmNew": {
            view: `${viewPrefix}EditRealmView`,
            url: /^realms\/new/,
            pattern: "realms/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsDashboard": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}dashboard/DashboardView`,
            url: defaultScopedByRealm("dashboard"),
            pattern: "realms/?/dashboard",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationSettings": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/SettingsView`,
            url: scopedByRealm("authentication-settings"),
            pattern: "realms/?/authentication-settings",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationChains": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/ChainsView`,
            url: scopedByRealm("authentication-chains"),
            pattern: "realms/?/authentication-chains",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationChainEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/chains/EditChainView`,
            url: scopedByRealm("authentication-chains/edit/([^/]+)"),
            pattern: "realms/?/authentication-chains/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin"
        },
        "realmsAuthenticationChainNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/chains/AddChainView`,
            url: scopedByRealm("authentication-chains/new"),
            pattern: "realms/?/authentication-chains/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },

        "realmsAuthenticationModules": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/ModulesView`,
            url: scopedByRealm("authentication-modules"),
            pattern: "realms/?/authentication-modules",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationModuleNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/modules/AddModuleView`,
            url: scopedByRealm("authentication-modules/new"),
            pattern: "realms/?/authentication-modules/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationModuleEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/modules/EditModuleView`,
            url: scopedByRealm("authentication-modules/([^/]+)/edit/([^/]+)"),
            pattern: "realms/?/authentication-modules/?/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationTrees": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/trees/list/ListTreesContainer`,
            url: scopedByRealm("authentication-trees"),
            pattern: "realms/?/authentication-trees",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationTreesNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authentication/trees/new/NewTreeContainer`,
            url: scopedByRealm("authentication-trees/new"),
            pattern: "realms/?/authentication-trees/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsAuthenticationTreesEdit": {
            view: `${viewPrefix}authentication/trees/edit/EditTreeContainer`,
            url: scopedByRealm("authentication-trees/edit/([^/]+)"),
            pattern: "realms/?/authentication-trees/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsServices": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}services/ServicesView`,
            url: scopedByRealm("services"),
            pattern: "realms/?/services",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsServiceEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}services/EditServiceView`,
            url: scopedByRealm("services/edit/([^/]+)"),
            pattern: "realms/?/services/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsServiceNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}services/NewServiceView`,
            url: scopedByRealm("services/new"),
            pattern: "realms/?/services/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsServiceSubSchemaNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}services/NewServiceSubSchemaView`,
            url: scopedByRealm("services/edit/([^/]+)/([^/]+)/new"),
            pattern: "realms/?/services/edit/?/?/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsServiceSubSchemaEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}services/EditServiceSubSchemaView`,
            url: scopedByRealm("services/edit/([^/]+)/([^/]+)/edit/([^/]+)"),
            pattern: "realms/?/services/edit/?/?/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsSessions": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}sessions/SessionsView`,
            url: scopedByRealm("sessions"),
            pattern: "realms/?/sessions",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsPolicySets": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/policySets/PolicySetsView`,
            url: scopedByRealm("authorization-policySets"),
            pattern: "realms/?/authorization-policySets",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsPolicySetEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/policySets/EditPolicySetView`,
            url: scopedByRealm("authorization-policySets/edit/([^/]+)"),
            pattern: "realms/?/authorization-policySets/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsPolicySetNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/policySets/EditPolicySetView`,
            url: scopedByRealm("authorization-policySets/new"),
            pattern: "realms/?/authorization-policySets/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsPolicyNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/policies/EditPolicyView`,
            url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/new"),
            pattern: "realms/?/authorization-policySets/edit/?/policies/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsPolicyEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/policies/EditPolicyView`,
            url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/edit/([^/]+)"),
            pattern: "realms/?/authorization-policySets/edit/?/policies/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsResourceTypes": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/resourceTypes/ResourceTypesView`,
            url: scopedByRealm("authorization-resourceTypes"),
            pattern: "realms/?/authorization-resourceTypes",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsResourceTypeEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/resourceTypes/EditResourceTypeView`,
            url: scopedByRealm("authorization-resourceTypes/edit/([^/]*)"),
            pattern: "realms/?/authorization-resourceTypes/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsResourceTypeNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}authorization/resourceTypes/EditResourceTypeView`,
            url: scopedByRealm("authorization-resourceTypes/new"),
            pattern: "realms/?/authorization-resourceTypes/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsScripts": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}scripts/ScriptsView`,
            url: scopedByRealm("scripts"),
            pattern: "realms/?/scripts",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsScriptEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}scripts/EditScriptView`,
            url: scopedByRealm("scripts/edit/([^/]*)"),
            pattern: "realms/?/scripts/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsScriptNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}scripts/EditScriptView`,
            url: scopedByRealm("scripts/new"),
            pattern: "realms/?/scripts/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsAgentsSelection": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/agents/SelectAgentView`,
            url: scopedByRealm("applications-agents/new"),
            pattern: "realms/?/applications-agents/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsAgentsNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/agents/NewAgentView`,
            url: scopedByRealm("applications-agents/new/([^/]*)"),
            pattern: "realms/?/applications-agents/new/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsOAuth2": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/oauth2/OAuth2`,
            url: scopedByRealm("applications-oauth2"),
            pattern: "realms/?/applications-oauth2",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsOAuth2ClientsNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/oauth2/clients/new/NewOAuth2ClientContainer`,
            url: scopedByRealm("applications-oauth2/clients/new"),
            pattern: "realms/?/applications-oauth2/clients/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsOAuth2ClientsEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/oauth2/clients/edit/EditOAuth2Client`,
            url: scopedByRealm("applications-oauth2/clients/edit/([^/]*)"),
            pattern: "realms/?/applications-oauth2/clients/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsOAuth2GroupsEdit": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/oauth2/groups/edit/EditOAuth2Group`,
            url: scopedByRealm("applications-oauth2/groups/edit/([^/]*)"),
            pattern: "realms/?/applications-oauth2/groups/edit/?",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        },
        "realmsApplicationsOAuth2GroupsNew": {
            view: `${viewPrefix}RealmTreeNavigationView`,
            page: `${viewPrefix}applications/oauth2/groups/new/NewOAuth2GroupContainer`,
            url: scopedByRealm("applications-oauth2/groups/new"),
            pattern: "realms/?/applications-oauth2/groups/new",
            role: "ui-realm-admin",
            navGroup: "admin",
            forceUpdate: true
        }
    };

    routes.realmDefault = routes.realmsDashboard;

    return routes;
});
