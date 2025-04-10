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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.service;

import static org.forgerock.openam.authentication.service.AuthModuleGlobalScript.AUTH_MODULE_SERVER_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_SERVER_SIDE;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.domain.LegacyScriptContextDetails;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.ScriptContextDetailsProvider;

import com.google.inject.Inject;
import com.iplanet.dpro.session.service.DsameAdminTokenProvider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.LdapSmsEntryUid;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManagementDAO;

/**
 * Responsible for providing the authentication module script contexts.
 */
public class AuthModuleScriptContextProvider implements ScriptContextDetailsProvider {

    private final DsameAdminTokenProvider dsameAdminTokenProvider;
    private final ServiceManagementDAO serviceManagementDAO;

    @Inject
    AuthModuleScriptContextProvider(DsameAdminTokenProvider dsameAdminTokenProvider,
            ServiceManagementDAO serviceManagementDAO) {
        this.dsameAdminTokenProvider = dsameAdminTokenProvider;
        this.serviceManagementDAO = serviceManagementDAO;
    }

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();

        scriptContexts.add(LegacyScriptContextDetails.builder()
                .withContextReference(AUTHENTICATION_SERVER_SIDE)
                .withI18NKey("script-type-02")
                .withDefaultScript(AUTH_MODULE_SERVER_SIDE.getId())
                .overrideDefaultWhiteList(
                        "com.sun.identity.shared.debug.Debug", "groovy.json.JsonSlurper", "java.lang.Boolean",
                        "java.lang.Byte", "java.lang.Character$Subset", "java.lang.Character$UnicodeBlock",
                        "java.lang.Character", "java.lang.Double", "java.lang.Float", "java.lang.Integer",
                        "java.lang.Long", "java.lang.Math", "java.lang.Number", "java.lang.Object", "java.lang.Short",
                        "java.lang.StrictMath", "java.lang.String", "java.lang.Void", "java.util.ArrayList$Itr",
                        "java.util.ArrayList", "java.util.HashMap$KeyIterator", "java.util.HashMap",
                        "java.util.HashSet", "java.util.LinkedHashMap", "java.util.LinkedHashSet",
                        "java.util.LinkedList", "java.util.TreeMap", "java.util.TreeSet",
                        "org.codehaus.groovy.runtime.GStringImpl", "org.codehaus.groovy.runtime.ScriptBytecodeAdapter",
                        "org.forgerock.http.client.*", "org.forgerock.http.protocol.Cookie",
                        "org.forgerock.http.protocol.Entity", "org.forgerock.http.protocol.Form",
                        "org.forgerock.http.protocol.Header", "org.forgerock.http.protocol.Headers",
                        "org.forgerock.http.protocol.Message", "org.forgerock.http.protocol.Request",
                        "org.forgerock.http.protocol.RequestCookies", "org.forgerock.http.protocol.Response",
                        "org.forgerock.http.protocol.ResponseException", "org.forgerock.http.protocol.Responses",
                        "org.forgerock.http.protocol.Status", "org.forgerock.json.JsonValue",
                        "org.forgerock.openam.authentication.modules.scripted.*",
                        "org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao",
                        "org.forgerock.openam.scripting.api.http.GroovyHttpClient",
                        "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
                        "org.forgerock.openam.scripting.api.identity.ScriptedIdentity",
                        "org.forgerock.openam.scripting.api.ScriptedSession",
                        "org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository",
                        "org.forgerock.openam.shared.security.crypto.CertificateService",
                        "org.forgerock.util.promise.NeverThrowsException", "org.forgerock.util.promise.Promise",
                        "org.forgerock.util.promise.PromiseImpl",
                        "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver",
                        "java.util.List", "java.util.Map", "java.util.Collections$UnmodifiableRandomAccessList",
                        "java.util.Collections$UnmodifiableCollection$1",
                        "org.mozilla.javascript.JavaScriptException",
                        "sun.security.ec.ECPrivateKeyImpl",
                        "org.forgerock.opendj.ldap.Rdn",
                        "org.forgerock.opendj.ldap.Dn"
                ).build());

        scriptContexts.add(LegacyScriptContextDetails.builder()
                .withContextReference(AUTHENTICATION_CLIENT_SIDE)
                .withI18NKey("script-type-03")
                .build());

        return scriptContexts;
    }

    @Override
    public long getUsageCount(Realm realm, Script script) {
        String uuid = script.getId();
        if (AUTHENTICATION_SERVER_SIDE.name().equals(script.getContext().name())) {
            return getUsageCount(getScriptedServiceBaseDN(realm), getServerSideScriptedAuthSearchString(uuid))
                    + getUsageCount(getDeviceIdMatchServiceBaseDN(realm), getServerSideScriptedAuthSearchString(uuid));
        } else if (AUTHENTICATION_CLIENT_SIDE.name().equals(script.getContext().name())) {
            return getUsageCount(getScriptedServiceBaseDN(realm), getClientSideScriptedAuthSearchString(uuid))
                    + getUsageCount(getDeviceIdMatchServiceBaseDN(realm), getClientSideScriptedAuthSearchString(uuid));
        }
        throw new IllegalArgumentException("Called getUsageCount on wrong provider");
    }

    private int getUsageCount(String dn, String search) {
        try {
            return serviceManagementDAO.search(getAdminToken(), LdapSmsEntryUid.of(dn), search, 0, 0, false, false).size();
        } catch (SMSException ignored) {
            // We get this exception if the LDAP entry we are searching for doesn't exist.
            // This can happen when we're looking for data under iPlanetAMAuthDeviceIdMatchService and we don't have
            // any device id match scripts.
        }
        return 0;
    }

    private SSOToken getAdminToken() {
        return dsameAdminTokenProvider.getAdminToken();
    }

    private String getServerSideScriptedAuthSearchString(String uuid) {
        return "(&(sunserviceID=serverconfig)(sunKeyValue=iplanet-am-auth-scripted-server-script=" + uuid + "))";
    }

    private String getScriptedServiceBaseDN(Realm realm) {
        return "ou=default,ou=OrganizationConfig,ou=1.0,ou=iPlanetAMAuthScriptedService,ou=services," + realm.asDN();
    }

    private String getDeviceIdMatchServiceBaseDN(Realm realm) {
        return "ou=default,ou=OrganizationConfig,ou=1.0,ou=iPlanetAMAuthDeviceIdMatchService,ou=services,"
                + realm.asDN();
    }

    private String getClientSideScriptedAuthSearchString(String uuid) {
        return "(&(sunserviceID=serverconfig)(sunKeyValue=iplanet-am-auth-scripted-client-script=" + uuid + "))";
    }
}
