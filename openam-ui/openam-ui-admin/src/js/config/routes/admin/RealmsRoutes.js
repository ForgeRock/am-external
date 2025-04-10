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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
/* eslint-disable max-len */

const scopedByRealm = (fragment) => new RegExp(`^realms/((?:%2F)[^/]*)/${fragment}$`);
const defaultScopedByRealm = (fragment) => scopedByRealm(`?(?:${fragment})?`);
const RealmsRoutes = {
    "realms": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/ListRealmsView.js"),
        url: /^realms\/*$/,
        pattern: "realms",
        role: "ui-realm-admin",
        navGroup: "admin"
    },
    "realmEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/EditRealmView.js"),
        url: scopedByRealm("edit"),
        pattern: "realms/?/edit",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/EditRealmView.js"),
        url: /^realms\/new/,
        pattern: "realms/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsDashboard": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardView.js"),
        url: defaultScopedByRealm("dashboard"),
        pattern: "realms/?/dashboard",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationSettings": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/SettingsView.js"),
        url: scopedByRealm("authentication-settings"),
        pattern: "realms/?/authentication-settings",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    "realmsAuthenticationTrees": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/trees/list/ListTreesContainer.jsx"),
        url: scopedByRealm("authentication-trees"),
        pattern: "realms/?/authentication-trees",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationTreesNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/trees/new/NewTreeContainer.jsx"),
        url: scopedByRealm("authentication-trees/new"),
        pattern: "realms/?/authentication-trees/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationTreesEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/EditTreeContainer.jsx"),
        url: scopedByRealm("authentication-trees/edit/([^/]+)"),
        pattern: "realms/?/authentication-trees/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationNodes": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/nodes/list/ListNodesContainer"),
        url: scopedByRealm("authentication-nodes"),
        pattern: "realms/?/authentication-nodes",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationNodesEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/nodes/edit/EditNodeContainer"),
        url: scopedByRealm("authentication-nodes/edit/([^/]+)"),
        pattern: "realms/?/authentication-nodes/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationNodesNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/nodes/new/NewNodeContainer"),
        url: scopedByRealm("authentication-nodes/new"),
        pattern: "realms/?/authentication-nodes/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationNodesImport": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/nodes/import/ImportNodesContainer"),
        url: scopedByRealm("authentication-nodes/import"),
        pattern: "realms/?/authentication-nodes/import",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationWebhooks": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/webhooks/list/ListWebhooksContainer"),
        url: scopedByRealm("authentication-webhooks"),
        pattern: "realms/?/authentication-webhooks",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationWebhooksEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/webhooks/edit/EditWebhookContainer"),
        url: scopedByRealm("authentication-webhooks/edit/([^/]+)"),
        pattern: "realms/?/authentication-webhooks/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsAuthenticationWebhooksNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authentication/webhooks/new/NewWebhookContainer"),
        url: scopedByRealm("authentication-webhooks/new"),
        pattern: "realms/?/authentication-webhooks/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsServices": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/services/ServicesView.js"),
        url: scopedByRealm("services"),
        pattern: "realms/?/services",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsServiceEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/services/EditServiceView.js"),
        url: scopedByRealm("services/edit/([^/]+)"),
        pattern: "realms/?/services/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsServiceNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/services/NewServiceView.js"),
        url: scopedByRealm("services/new"),
        pattern: "realms/?/services/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsServiceSubSchemaNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/services/NewServiceSubSchemaView.js"),
        url: scopedByRealm("services/edit/([^/]+)/([^/]+)/new"),
        pattern: "realms/?/services/edit/?/?/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsServiceSubSchemaEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/services/EditServiceSubSchemaView.js"),
        url: scopedByRealm("services/edit/([^/]+)/([^/]+)/edit/([^/]+)"),
        pattern: "realms/?/services/edit/?/?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsSessions": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/sessions/SessionsView.jsx"),
        url: scopedByRealm("sessions"),
        pattern: "realms/?/sessions",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsPolicySets": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/policySets/PolicySetsView.js"),
        url: scopedByRealm("authorization-policySets"),
        pattern: "realms/?/authorization-policySets",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsPolicySetEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView.js"),
        url: scopedByRealm("authorization-policySets/edit/([^/]+)"),
        pattern: "realms/?/authorization-policySets/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsPolicySetNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView.js"),
        url: scopedByRealm("authorization-policySets/new"),
        pattern: "realms/?/authorization-policySets/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsPolicyNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView.js"),
        url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/new"),
        pattern: "realms/?/authorization-policySets/edit/?/policies/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsPolicyEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView.js"),
        url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/edit/([^/]+)"),
        pattern: "realms/?/authorization-policySets/edit/?/policies/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsResourceTypes": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypesView.js"),
        url: scopedByRealm("authorization-resourceTypes"),
        pattern: "realms/?/authorization-resourceTypes",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsResourceTypeEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView.js"),
        url: scopedByRealm("authorization-resourceTypes/edit/([^/]*)"),
        pattern: "realms/?/authorization-resourceTypes/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsResourceTypeNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView.js"),
        url: scopedByRealm("authorization-resourceTypes/new"),
        pattern: "realms/?/authorization-resourceTypes/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsScripts": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/scripts/ScriptsView.js"),
        url: scopedByRealm("scripts"),
        pattern: "realms/?/scripts",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsScriptEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView.js"),
        url: scopedByRealm("scripts/edit/([^/]*)"),
        pattern: "realms/?/scripts/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsScriptNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView.js"),
        url: scopedByRealm("scripts/new"),
        pattern: "realms/?/scripts/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Applications => OAuth 2.0 => Clients
    "realmsApplicationsOAuth2Clients": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/clients/OAuth2Clients.jsx"),
        url: scopedByRealm("applications-oauth2-clients"),
        pattern: "realms/?/applications-oauth2-clients",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2ClientsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/clients/clients/new/NewOAuth2ClientContainer.jsx"),
        url: scopedByRealm("applications-oauth2-clients/clients/new"),
        pattern: "realms/?/applications-oauth2-clients/clients/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2ClientsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/clients/clients/edit/EditOAuth2Client.js"),
        url: scopedByRealm("applications-oauth2-clients/clients/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-clients/clients/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2GroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/clients/groups/edit/EditOAuth2Group.js"),
        url: scopedByRealm("applications-oauth2-clients/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-clients/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2GroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/clients/groups/new/NewOAuth2GroupContainer.jsx"),
        url: scopedByRealm("applications-oauth2-clients/groups/new"),
        pattern: "realms/?/applications-oauth2-clients/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Applications => OAuth 2.0 => Remote Consent
    "realmsApplicationsOAuth2RemoteConsent": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/remote-consent/RemoteConsentAgents.jsx"),
        url: scopedByRealm("applications-oauth2-remoteConsent"),
        pattern: "realms/?/applications-oauth2-remoteConsent",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2RemoteConsentAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/remote-consent/agents/new/NewRemoteConsentAgentContainer.jsx"),
        url: scopedByRealm("applications-oauth2-remoteConsent/agents/new"),
        pattern: "realms/?/applications-oauth2-remoteConsent/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2RemoteConsentAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/remote-consent/agents/edit/EditRemoteConsentAgent.js"),
        url: scopedByRealm("applications-oauth2-remoteConsent/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-remoteConsent/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2RemoteConsentAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/remote-consent/groups/new/NewRemoteConsentAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-oauth2-remoteConsent/groups/new"),
        pattern: "realms/?/applications-oauth2-remoteConsent/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2RemoteConsentAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/remote-consent/groups/edit/EditRemoteConsentAgentGroup.js"),
        url: scopedByRealm("applications-oauth2-remoteConsent/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-remoteConsent/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Applications => OAuth 2.0 => Software Publisher
    "realmsApplicationsOAuth2SoftwarePublisher": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/software-publisher/SoftwarePublisherAgents.jsx"),
        url: scopedByRealm("applications-oauth2-softwarePublisher"),
        pattern: "realms/?/applications-oauth2-softwarePublisher",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2SoftwarePublisherAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/software-publisher/agents/new/NewSoftwarePublisherAgentContainer.jsx"),
        url: scopedByRealm("applications-oauth2-softwarePublisher/agents/new"),
        pattern: "realms/?/applications-oauth2-softwarePublisher/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2SoftwarePublisherAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/software-publisher/agents/edit/EditSoftwarePublisherAgent.js"),
        url: scopedByRealm("applications-oauth2-softwarePublisher/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-softwarePublisher/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2SoftwarePublisherAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/software-publisher/groups/new/NewSoftwarePublisherAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-oauth2-softwarePublisher/groups/new"),
        pattern: "realms/?/applications-oauth2-softwarePublisher/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2SoftwarePublisherAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/software-publisher/groups/edit/EditSoftwarePublisherAgentGroup.js"),
        url: scopedByRealm("applications-oauth2-softwarePublisher/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-softwarePublisher/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    // Applications => OAuth 2.0 => Trusted JWT Issuer
    "realmsApplicationsOAuth2TrustedJwtIssuer": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/trusted-jwt-issuer/TrustedJwtIssuerAgents.jsx"),
        url: scopedByRealm("applications-oauth2-trustedJwtIssuer"),
        pattern: "realms/?/applications-oauth2-trustedJwtIssuer",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2TrustedJwtIssuerAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/trusted-jwt-issuer/agents/new/NewTrustedJwtIssuerAgentContainer.jsx"),
        url: scopedByRealm("applications-oauth2-trustedJwtIssuer/agents/new"),
        pattern: "realms/?/applications-oauth2-trustedJwtIssuer/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2TrustedJwtIssuerAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/trusted-jwt-issuer/agents/edit/EditTrustedJwtIssuerAgent.js"),
        url: scopedByRealm("applications-oauth2-trustedJwtIssuer/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-trustedJwtIssuer/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2TrustedJwtIssuerAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/trusted-jwt-issuer/groups/new/NewTrustedJwtIssuerAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-oauth2-trustedJwtIssuer/groups/new"),
        pattern: "realms/?/applications-oauth2-trustedJwtIssuer/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsOAuth2TrustedJwtIssuerAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/oauth2/trusted-jwt-issuer/groups/edit/EditTrustedJwtIssuerAgentGroup.js"),
        url: scopedByRealm("applications-oauth2-trustedJwtIssuer/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-oauth2-trustedJwtIssuer/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    "realmsApplicationsAgentsWeb": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/web/WebAgents.jsx"),
        url: scopedByRealm("applications-agents-web"),
        pattern: "realms/?/applications-agents-web",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsWebAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/web/agents/new/NewWebAgentContainer.jsx"),
        url: scopedByRealm("applications-agents-web/agents/new"),
        pattern: "realms/?/applications-agents-web/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsWebAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/web/agents/edit/EditWebAgent.js"),
        url: scopedByRealm("applications-agents-web/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-web/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsWebAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/web/groups/new/NewWebAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-agents-web/groups/new"),
        pattern: "realms/?/applications-agents-web/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsWebAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/web/groups/edit/EditWebAgentGroup.js"),
        url: scopedByRealm("applications-agents-web/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-web/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsJava": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/java/JavaAgents.jsx"),
        url: scopedByRealm("applications-agents-java"),
        pattern: "realms/?/applications-agents-java",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsJavaAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/java/agents/new/NewJavaAgentContainer.jsx"),
        url: scopedByRealm("applications-agents-java/agents/new"),
        pattern: "realms/?/applications-agents-java/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsJavaAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/java/agents/edit/EditJavaAgent.js"),
        url: scopedByRealm("applications-agents-java/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-java/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsJavaAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/java/groups/edit/EditJavaAgentGroup.js"),
        url: scopedByRealm("applications-agents-java/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-java/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsJavaAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/java/groups/new/NewJavaAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-agents-java/groups/new"),
        pattern: "realms/?/applications-agents-java/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsIdentityGateway": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/identity-gateway/IdentityGatewayAgents.jsx"),
        url: scopedByRealm("applications-agents-identityGateway"),
        pattern: "realms/?/applications-agents-identityGateway",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsIdentityGatewayAgentsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/identity-gateway/agents/new/NewIdentityGatewayAgentContainer.jsx"),
        url: scopedByRealm("applications-agents-identityGateway/agents/new"),
        pattern: "realms/?/applications-agents-identityGateway/agents/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsIdentityGatewayAgentsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/identity-gateway/agents/edit/EditIdentityGatewayAgent.js"),
        url: scopedByRealm("applications-agents-identityGateway/agents/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-identityGateway/agents/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsIdentityGatewayAgentGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/identity-gateway/groups/edit/EditIdentityGatewayAgentGroup.js"),
        url: scopedByRealm("applications-agents-identityGateway/groups/edit/([^/]*)"),
        pattern: "realms/?/applications-agents-identityGateway/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsAgentsIdentityGatewayAgentGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/agents/identity-gateway/groups/new/NewIdentityGatewayAgentGroupContainer.jsx"),
        url: scopedByRealm("applications-agents-identityGateway/groups/new"),
        pattern: "realms/?/applications-agents-identityGateway/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Applications => Federation => Circles of Trust
    "realmsApplicationsFederationCirclesOfTrust": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/circlesoftrust/list/ListCirclesOfTrustContainer.jsx"),
        url: scopedByRealm("applications-federation-circlesOfTrust"),
        pattern: "realms/?/applications-federation-circlesOfTrust",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsFederationCirclesOfTrustNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/circlesoftrust/new/NewCircleOfTrustContainer.jsx"),
        url: scopedByRealm("applications-federation-circlesOfTrust/new"),
        pattern: "realms/?/applications-federation-circlesOfTrust/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsApplicationsFederationCirclesOfTrustEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/circlesoftrust/edit/EditCircleOfTrust.js"),
        url: scopedByRealm("applications-federation-circlesOfTrust/edit/([^/]*)"),
        pattern: "realms/?/applications-federation-circlesOfTrust/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Applications => Federation => Entity Providers
    "realmsApplicationsFederationEntityProviders": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/entityproviders/list/ListEntityProvidersContainer.jsx"),
        url: scopedByRealm("applications-federation-entityProviders"),
        pattern: "realms/?/applications-federation-entityProviders",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    "realmsApplicationsFederationEntityProvidersEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/entityproviders/edit/EditEntityProviderContainer.jsx"),
        url: scopedByRealm("applications-federation-entityProviders/([^/]*)-([^/]*)/edit/([^/]*)"),
        pattern: "realms/?/applications-federation-entityProviders/?-?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    "realmsApplicationsFederationEntityProvidersNewHosted": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/entityproviders/new/NewHostedEntityProviderContainer.jsx"),
        url: scopedByRealm("applications-federation-entityProviders/hosted/new/?([^/]*)"),
        pattern: "realms/?/applications-federation-entityProviders/hosted/new/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    "realmsApplicationsFederationEntityProvidersNewRemote": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/applications/federation/entityproviders/new/NewRemoteEntityProviderContainer.jsx"),
        url: scopedByRealm("applications-federation-entityProviders/remote/new/?([^/]*)"),
        pattern: "realms/?/applications-federation-entityProviders/remote/new/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },

    // Identity Stores
    "realmsDataStores": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/datastores/list/ListDataStoresContainer.jsx"),
        url: scopedByRealm("identityStores"),
        pattern: "realms/?/identityStores",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsDataStoresEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/datastores/edit/EditDataStore.js"),
        url: scopedByRealm("identityStores/([^/]*)/edit/([^/]*)"),
        pattern: "realms/?/identityStores/?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsDataStoresNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/datastores/new/NewDataStoreContainer.jsx"),
        url: scopedByRealm("identityStores/new"),
        pattern: "realms/?/identityStores/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentities": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/Identities.jsx"),
        url: scopedByRealm("identities"),
        pattern: "realms/?/identities",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesUsersEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/users/edit/EditUser.jsx"),
        url: scopedByRealm("identities/edit/([^/]*)"),
        pattern: "realms/?/identities/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesUsersNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/users/new/NewUserContainer.jsx"),
        url: scopedByRealm("identities/new"),
        pattern: "realms/?/identities/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesUsersServicesNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/users/edit/services/new/NewUserServiceContainer.jsx"),
        url: scopedByRealm("identities/edit/([^/]*)/services/new/([^/]*)"),
        pattern: "realms/?/identities/edit/?/services/new/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesUsersServicesEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/users/edit/services/edit/EditUserService.js"),
        url: scopedByRealm("identities/edit/([^/]*)/services/edit/([^/]*)"),
        pattern: "realms/?/identities/edit/?/services/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesGroupsEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/groups/edit/EditGroupContainer.jsx"),
        url: scopedByRealm("identities/groups/edit/([^/]*)"),
        pattern: "realms/?/identities/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesAllAuthenticatedEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/groups/edit/EditAllAuthenticatedContainer.jsx"),
        url: scopedByRealm("identities/groups/edit/allAuthenticatedIdentities"),
        pattern: "realms/?/identities/groups/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsIdentitiesGroupsNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/identities/groups/new/NewGroupContainer.jsx"),
        url: scopedByRealm("identities/groups/new"),
        pattern: "realms/?/identities/groups/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsSts": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/sts/STS.jsx"),
        url: scopedByRealm("sts"),
        pattern: "realms/?/sts",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsStsRestNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/sts/rest/new/NewRestSTSContainer.jsx"),
        url: scopedByRealm("sts/rest/new"),
        pattern: "realms/?/sts/rest/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsStsRestEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/sts/rest/edit/EditRestSTS.js"),
        url: scopedByRealm("sts/rest/edit/([^/]*)"),
        pattern: "realms/?/sts/rest/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsSecretStores": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/secretStores/list/ListSecretStoresContainer.jsx"),
        url: scopedByRealm("secretStores"),
        pattern: "realms/?/secretStores",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsSecretStoresNew": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/secretStores/new/NewSecretStoresContainer.jsx"),
        url: scopedByRealm("secretStores/new"),
        pattern: "realms/?/secretStores/new",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    },
    "realmsSecretStoresEdit": {
        view: () => import("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView.js"),
        page: () => import("org/forgerock/openam/ui/admin/views/realms/secretStores/edit/EditSecretStoreContainer.jsx"),
        url: scopedByRealm("secretStores/([^/]*)/edit/([^/]*)"),
        pattern: "realms/?/secretStores/?/edit/?",
        role: "ui-realm-admin",
        navGroup: "admin",
        forceUpdate: true
    }
};

RealmsRoutes.realmDefault = RealmsRoutes.realmsDashboard;

export default RealmsRoutes;
