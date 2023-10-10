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
 * Copyright 2020-2023 ForgeRock AS.
 */
package org.forgerock.openam.service.datastore;

import static com.sun.identity.idm.IdConstants.AGENT_SERVICE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.core.realms.impl.NoRealmFoundException;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreService;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConstraintViolationException;
import org.forgerock.opendj.ldap.Dn;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.messages.AddRequest;
import org.forgerock.opendj.ldif.LdifEntryReader;
import org.forgerock.util.Action;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.DataStoreInitializer;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Initialises DataStores.
 *
 * <p>This is used on defining new DataStores at runtime when creating new realms. The DataStore service is copied,
 * if present, to the new child realm and the configured DataStores are initialised for the new realm.</p>
 */
public class DefaultDataStoreInitializer implements DataStoreInitializer {

    private static final String AGENT_GROUP = "agentgroup";
    private static final String INSTANCES_NODE = "ou=Instances,";

    private final DataStoreService dataStoreService;
    private final RealmLookup realmLookup;

    private static final String HIDDEN_POLICY_LDIF = "ldif/hiddenrealm-policy.ldif";
    private static final String ROOT_POLICY_LDIF = "ldif/root-policy.ldif";
    private static final String ROOT_APPLICATION_LDIF = "ldif/root-application.ldif";
    private static final String SUB_REALM_POLICY_LDIF = "ldif/subrealm-policy.ldif";
    private static final String SUB_REALM_APPLICATION_LDIF = "ldif/subrealm-application.ldif";

    @Inject
    DefaultDataStoreInitializer(DataStoreService dataStoreService, RealmLookup realmLookup) {
        this.dataStoreService = dataStoreService;
        this.realmLookup = realmLookup;
    }

    @Override
    public void handlePolicyDataStoreCreation(SSOToken token, Realm realm, DataStoreId dataStoreId)
            throws SMSException, RealmLookupException {
        if (realmLookup.lookupAsOptional(realm.asPath()).isEmpty()) {
            throw new NoRealmFoundException(realm.asPath());
        }

        try (Connection connection = dataStoreService.getConnectionFactory(dataStoreId).getConnection()) {
            if (isRootRealm(realm)) {
                realm = Realms.root(); // handle bad dn (OPENAM-21108)
                applyLdif(realm, connection, HIDDEN_POLICY_LDIF);
                applyLdif(realm, connection, ROOT_POLICY_LDIF);
            } else {
                Dn realmDn = Dn.valueOf(realm.asDN());
                Dn rootServicesDn = Dn.valueOf(SMSEntry.SERVICES_RDN + "," + Realms.root().asDN());
                createRealmHierarchy(connection, realmDn, rootServicesDn);
                applyLdif(realm, connection, SUB_REALM_POLICY_LDIF);
            }
        } catch (IOException e) {
            throw new SMSException(e.getMessage(), e, "sms-entry-cannot-create");
        }
    }

    @Override
    public void handleApplicationDataStoreCreation(SSOToken token, Realm realm, DataStoreId dataStoreId)
            throws SSOException, SMSException, RealmLookupException {
        if (realmLookup.lookupAsOptional(realm.asPath()).isEmpty()) {
            throw new NoRealmFoundException(realm.asPath());
        }

        try (Connection connection = dataStoreService.getConnectionFactory(dataStoreId).getConnection()) {
            if (isRootRealm(realm)) {
                realm = Realms.root(); // handle bad dn (OPENAM-21108)
                applyLdif(realm, connection, ROOT_APPLICATION_LDIF);
            } else {
                Dn realmDn = Dn.valueOf(realm.asDN());
                Dn rootServicesDn = Dn.valueOf(SMSEntry.SERVICES_RDN + "," + Realms.root().asDN());

                createRealmHierarchy(connection, realmDn, rootServicesDn);
                applyLdif(realm, connection, SUB_REALM_APPLICATION_LDIF);
            }
            initializeAgentGroupFBCEntry(dataStoreId, realm, token);
        } catch (IOException e) {
            throw new SMSException(e.getMessage(), e, "sms-entry-cannot-create");
        }
    }

    private void initializeAgentGroupFBCEntry(DataStoreId applicationDataStoreId, Realm realm, SSOToken token) throws SMSException, SSOException {
        if (!isCustomDataStore(applicationDataStoreId)) {
            // This code is needed to create the agent group entry in FBC in the default datastore case
            // It may be removed once AME-24590 is resolved
            final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(AGENT_SERVICE, token);
            final ServiceConfig orgConfig = serviceConfigManager.getOrganizationConfig(realm.asPath(), null,
                    applicationDataStoreId);
            createAgentGroupInstancesNode(realm, applicationDataStoreId, orgConfig);
        }
    }

    private void applyLdif(Realm realm, Connection connection, String ldifFile) throws IOException {
        LdifEntryReader entryReader = loadLdif(realm, ldifFile);
        writeEntries(connection, entryReader);
    }

    private void createRealmHierarchy(Connection connection, Dn realmDn, Dn rootServicesDn) throws IOException {
        if (realmDn.equals(rootServicesDn)) {
            AddRequest addRequest = LDAPRequests.newAddRequest(realmDn)
                    .addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP, SMSEntry.OC_ORG_UNIT);
            ignoreAlreadyExistsException(() -> connection.add(addRequest));
            return;
        }

        createRealmHierarchy(connection, realmDn.parent(), rootServicesDn);
        AddRequest addRequest = LDAPRequests.newAddRequest(realmDn)
                .addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP, SMSEntry.OC_REALM_SERVICE);
        ignoreAlreadyExistsException(() -> connection.add(addRequest));
    }

    private void writeEntries(Connection connection, LdifEntryReader entries) throws IOException {
        while (entries.hasNext()) {
            ignoreAlreadyExistsException(() -> connection.add(entries.readEntry()));
        }
    }

    private void ignoreAlreadyExistsException(Action<IOException> ldapAction) throws IOException {
        try {
            ldapAction.run();
        } catch (ConstraintViolationException e) {
            if (!e.getResult().getResultCode().equals(ResultCode.ENTRY_ALREADY_EXISTS)) {
                throw e;
            }
            // ignore.. already exists
        }
    }

    private LdifEntryReader loadLdif(Realm realm, String ldifFile) throws IOException {
        InputStream inputStream = DefaultDataStoreInitializer.class.getClassLoader().getResourceAsStream(ldifFile);
        InputStream tagSwappedStream = tagSwapStream(realm, inputStream);
        return new LdifEntryReader(tagSwappedStream);
    }

    private InputStream tagSwapStream(Realm realm, InputStream inputStream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        StringBuilder stringBuilder = new StringBuilder();
        char[] charBuffer = new char[1024];
        int length;
        while ((length = inputStreamReader.read(charBuffer)) > 0) {
            stringBuilder.append(charBuffer, 0, length);
        }
        String data = StringUtils.tagSwap(stringBuilder.toString(), getTags(realm));
        return new ByteArrayInputStream(data.getBytes());
    }

    private Map<String, String> getTags(Realm realm) {
        Map<String, String> tags = new HashMap<>(2);
        tags.put("@SM_ROOT_SUFFIX_HAT@", SMSEntry.baseDN.replaceAll(",", "^").trim());
        tags.put("@ROOT_SUFFIX@", SMSEntry.baseDN);
        tags.put("@REALM_SUFFIX@", realm.asDN());
        return tags;
    }

    private boolean isCustomDataStore(DataStoreId applicationDataStoreId) {
        return !DataStoreId.CONFIG.equals(applicationDataStoreId);
    }

    private void createAgentGroupInstancesNode(Realm realm, DataStoreId dataStoreId, ServiceConfig orgConfig)
            throws SSOException, SMSException {
        final String instancesAgentGroupDN = constructAgentGroupDn(INSTANCES_NODE, realm);
        orgConfig.checkAndCreateGroup(instancesAgentGroupDN, AGENT_GROUP, dataStoreId);
    }

    private String constructAgentGroupDn(String node, Realm realm) {
        return "ou=agentgroup," + node + "ou=1.0,ou=AgentService,ou=services," + realm.asDN();
    }

    private boolean isRootRealm(Realm realm) {
        return Realms.root().equals(realm);
    }
}
