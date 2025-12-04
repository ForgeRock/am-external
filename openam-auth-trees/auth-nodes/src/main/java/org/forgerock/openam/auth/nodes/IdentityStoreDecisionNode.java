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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.ldap.LDAPConstants.BEHERA_SUPPORT_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_AFFINITY_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_AFFINITY_LEVEL;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_MODE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_TRUST_ALL_SERVER_CERTIFICATES;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CREATION_ATTR_MAPPING;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PEOPLE_CONTAINER_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PEOPLE_CONTAINER_VALUE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SEARCH_SCOPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_HEARTBEAT_INTERVAL;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_HEARTBEAT_TIME_UNIT;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_PASSWORD;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_ROOT_SUFFIX;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_USER_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_TIME_LIMIT;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_SEARCH_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.MTLS_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.MTLS_SECRET_LABEL;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.AffinityLevel;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapper;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapperFactory;
import org.forgerock.openam.ldap.LDAPConstants;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.common.IdRepoUtilityService;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * A node that decides if the username and password exist in the LDAP database.
 * Expects 'username' and 'password' fields to be present in the shared state.
 * The node reads its configuration from the existing OpenDJ Identity Store.
 */
@Node.Metadata(outcomeProvider = LdapDecisionNode.LdapOutcomeProvider.class,
        configClass = IdentityStoreDecisionNode.Config.class,
        tags = {"basic authn", "basic authentication"})
public class IdentityStoreDecisionNode extends LdapDecisionNode {

    private static final Logger logger = LoggerFactory.getLogger(IdentityStoreDecisionNode.class);
    private final Config config;

    /**
     * The interface Config.
     */
    public interface Config {

        /**
         * It returns LdapConfig class which implements LdapDecisionNode.Config interface.
         *
         * @param configManagerFactory the ServiceConfigManagerFactory.
         * @param idRepoUtilityService the IdRepo utilities.
         * @return the LdapDecisionNode Config.
         * @throws NodeProcessException When Identity Store configuration cannot be read from the database.
         */
        default LdapDecisionNode.Config getConfig(ServiceConfigManagerFactory configManagerFactory,
                IdRepoUtilityService idRepoUtilityService) throws NodeProcessException {
            try {
                ServiceConfigManager configManager = configManagerFactory.create(IdConstants.REPO_SERVICE, "1.0");
                return new LdapConfig(configManager, minimumPasswordLength(), mixedCaseForPasswordChangeMessages(),
                        idRepoUtilityService);
            } catch (SMSException | SSOException e) {
                throw new NodeProcessException("Failed to get IdRepo configuration", e);
            }
        }

        /**
         * The smallest possible length of the password.
         * If this value is set to 0 the minimum password length is not checked.
         *
         * @return minimum password length.
         */
        @Attribute(order = 200)
        default int minimumPasswordLength() {
            return 8;
        }

        /**
         * Allows for the username to be represented by the user's universal id.
         *
         * @return {@code false} if the username should be represented by the username value,
         * or {@code true} if the username should be represented by the universal id value
         */
        @Attribute(order = 300)
        default boolean useUniversalIdForUsername() {
            return false;
        }

        /**
         * Determines whether mixed case should be used for password change messages returned from this node.
         * Previously all messages were returned uppercase.
         *
         * @return <code>true</code> to enable the use of mixed case messages.
         */
        @Attribute(order = 400)
        default boolean mixedCaseForPasswordChangeMessages() {
            return false;
        }
    }

    /**
     * Constructs a new {@link IdentityStoreDecisionNode}.
     *
     * @param config                               provides the settings for initialising an
     *                                             {@link IdentityStoreDecisionNode}.
     * @param realm                                The realm.
     * @param configManagerFactory                 A ServiceConfigManagerFactory instance.
     * @param coreWrapper                          A core wrapper instance.
     * @param identityService                      An IdentityService instance.
     * @param idRepoUtilityService                 the IdRepo utilities.
     * @param secrets                              A Secrets instance.
     * @param connectionFactoryAuditWrapperFactory A factory for creating {@link ConnectionFactoryAuditWrapper}.
     * @param accountLockoutTreesFactory            A factory for creating {@link AMAccountLockoutTrees}.
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public IdentityStoreDecisionNode(@Assisted Config config, @Assisted Realm realm,
            ServiceConfigManagerFactory configManagerFactory, CoreWrapper coreWrapper,
            LegacyIdentityService identityService, IdRepoUtilityService idRepoUtilityService, Secrets secrets,
            ConnectionFactoryAuditWrapperFactory connectionFactoryAuditWrapperFactory, AMAccountLockoutTrees.Factory
                    accountLockoutTreesFactory)
            throws NodeProcessException {
        super(config.getConfig(configManagerFactory, idRepoUtilityService), realm, coreWrapper, identityService,
                secrets, connectionFactoryAuditWrapperFactory, accountLockoutTreesFactory);
        this.config = config;
    }

    @Override
    protected boolean isUsernameRepresentedByUniversalId() {
        return config.useUniversalIdForUsername();
    }

    private static final class LdapConfig implements LdapDecisionNode.Config {

        private static final Set<String> ALLOWED_ID_REPO_TYPES = Set.of(
                "LDAPv3", "LDAPv3ForAMDS", "LDAPv3ForOpenDS", "LDAPv3ForForgeRockIAM"
        );

        private final ServiceConfigManager configManager;
        private final int minimumPasswordLength;
        private boolean mixedCaseForPasswordChangeMessages;
        private final IdRepoUtilityService idRepoUtilityService;
        private ConfigurationAttributes attributes;


        private LdapConfig(ServiceConfigManager configManager, int minimumPasswordLength,
                           boolean mixedCaseForPasswordChangeMessages,
                           IdRepoUtilityService idRepoUtilityService) {
            this.configManager = configManager;
            this.minimumPasswordLength = minimumPasswordLength;
            this.mixedCaseForPasswordChangeMessages = mixedCaseForPasswordChangeMessages;
            this.idRepoUtilityService = idRepoUtilityService;
        }

        @Override
        public void initialise(TreeContext context) throws NodeProcessException {
            if (attributes == null) {
                try {
                    ServiceConfig serviceConfig = configManager.getOrganizationConfig(
                            context.sharedState.get(REALM).asString(), null);
                    final ServiceConfig subConfig = getDefaultIdentityStore(serviceConfig);
                    attributes = subConfig.getAttributesForRead();
                } catch (SMSException | SSOException e) {
                    throw new NodeProcessException("Failed to get IdRepo config", e);
                }
            }
        }

        private ServiceConfig getDefaultIdentityStore(ServiceConfig serviceConfig) throws SMSException, SSOException,
                NodeProcessException {
            final Set<ServiceConfig> allowedIdRepos = new HashSet<>();
            final Set<String> identityStoreNames = serviceConfig.getSubConfigNames();
            for (String idRepoName : identityStoreNames) {
                ServiceConfig subConfig = serviceConfig.getSubConfig(idRepoName);
                if (ALLOWED_ID_REPO_TYPES.contains(subConfig.getSchemaID())) {
                    allowedIdRepos.add(subConfig);
                }
            }
            if (allowedIdRepos.size() != 1) {
                throw new NodeProcessException("Must have exactly 1 allowed identity store configured to"
                        + " use as default.");
            }
            return allowedIdRepos.iterator().next();
        }

        @Override
        public Set<String> primaryServers() {
            return idRepoUtilityService.getPrioritizedLDAPUrls(
                            attributes.get(LDAPConstants.LDAP_SERVER_LIST))
                    .stream()
                    .map(LDAPURL::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public Set<String> secondaryServers() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> accountSearchBaseDn() {
            String peopleContainerName = CollectionHelper.getMapAttr(attributes,
                    LDAP_PEOPLE_CONTAINER_NAME, null);
            String peopleContainerValue = CollectionHelper.getMapAttr(attributes,
                    LDAP_PEOPLE_CONTAINER_VALUE, null);
            if (StringUtils.isNotBlank(peopleContainerName) && StringUtils.isNotBlank(peopleContainerValue)) {
                String peopleContainerRdn = peopleContainerName + "=" + peopleContainerValue;
                return attributes.get(LDAP_SERVER_ROOT_SUFFIX)
                        .stream().map(name -> peopleContainerRdn + "," + name)
                        .collect(Collectors.toSet());
            }

            return attributes.get(LDAP_SERVER_ROOT_SUFFIX);
        }

        @Override
        public Optional<String> adminDn() {
            return Optional.ofNullable(CollectionHelper.getMapAttr(attributes, LDAP_SERVER_USER_NAME));
        }

        @Override
        public Optional<char[]> adminPassword() {
            final String value = CollectionHelper.getMapAttr(attributes, LDAP_SERVER_PASSWORD);
            return value != null ? Optional.of(value.toCharArray()) : Optional.empty();
        }

        @Override
        public String userProfileAttribute() {
            return CollectionHelper.getMapAttr(attributes, LDAP_USER_NAMING_ATTR);
        }

        @Override
        public Set<String> searchFilterAttributes() {
            return Set.of(CollectionHelper.getMapAttr(attributes, LDAP_USER_NAMING_ATTR));
        }

        @Override
        public Optional<String> userSearchFilter() {
            return Optional.ofNullable(CollectionHelper.getMapAttr(attributes, LDAP_USER_SEARCH_FILTER));
        }

        @Override
        public LdapDecisionNode.SearchScope searchScope() {
            String searchScope = CollectionHelper.getMapAttr(attributes, LDAP_SEARCH_SCOPE);
            switch (searchScope) {
            case "SCOPE_BASE":
                return SearchScope.OBJECT;
            case "SCOPE_ONE":
                return SearchScope.ONE_LEVEL;
            default:
                return SearchScope.SUBTREE;
            }
        }

        @Override
        public LdapDecisionNode.LdapConnectionMode ldapConnectionMode() {
            String connectionMode = CollectionHelper.getMapAttr(attributes, LDAP_CONNECTION_MODE);
            if ("StartTLS".equals(connectionMode)) {
                return LdapDecisionNode.LdapConnectionMode.START_TLS;
            } else {
                return LdapDecisionNode.LdapConnectionMode.valueOf(connectionMode);
            }
        }

        @Override
        public boolean mtlsEnabled() {
            return CollectionHelper.getBooleanMapAttr(attributes, MTLS_ENABLED, false);
        }

        @Override
        public Optional<String> mtlsSecretLabel() {
            return Optional.ofNullable(CollectionHelper.getMapAttr(attributes, MTLS_SECRET_LABEL));
        }

        @Override
        public Set<String> userCreationAttrs() {
            return attributes.get(LDAP_CREATION_ATTR_MAPPING);
        }

        @Override
        public int minimumPasswordLength() {
            return minimumPasswordLength;
        }

        @Override
        public boolean mixedCaseForPasswordChangeMessages() {
            return mixedCaseForPasswordChangeMessages;
        }

        @Override
        public boolean beheraEnabled() {
            return CollectionHelper.getBooleanMapAttr(attributes, BEHERA_SUPPORT_ENABLED,
                    LdapDecisionNode.Config.super.beheraEnabled());
        }

        @Override
        public boolean trustAllServerCertificates() {
            return CollectionHelper.getBooleanMapAttr(attributes,
                    LDAP_CONNECTION_TRUST_ALL_SERVER_CERTIFICATES, false);
        }

        @Override
        public int heartbeatInterval() {
            return CollectionHelper.getIntMapAttr(attributes, LDAP_SERVER_HEARTBEAT_INTERVAL,
                    LdapDecisionNode.Config.super.heartbeatInterval(), logger);
        }

        @Override
        public LdapDecisionNode.HeartbeatTimeUnit heartbeatTimeUnit() {
            return LdapDecisionNode.HeartbeatTimeUnit.valueOf(
                    CollectionHelper.getServerMapAttr(attributes, LDAP_SERVER_HEARTBEAT_TIME_UNIT));
        }

        @Override
        public int ldapOperationsTimeout() {
            return CollectionHelper.getIntMapAttr(attributes, LDAP_TIME_LIMIT,
                    LdapDecisionNode.Config.super.ldapOperationsTimeout(), logger);
        }

        @Override
        public Set<String> additionalPasswordChangeSearchAttributes() {
            String ldapUserSearchAttribute = CollectionHelper.getMapAttr(attributes, LDAP_USER_SEARCH_ATTR);
            return ldapUserSearchAttribute == null ? Collections.emptySet() : Set.of(ldapUserSearchAttribute);
        }

        @Override
        public AffinityLevel affinityLevel() {
            if (CollectionHelper.getBooleanMapAttr(attributes, LDAP_CONNECTION_AFFINITY_ENABLED, false)) {
                return AffinityLevel.fromConfigValue(CollectionHelper.getMapAttr(attributes,
                        LDAP_CONNECTION_AFFINITY_LEVEL));
            }
            return AffinityLevel.NONE;
        }
    }

    @Override
    protected String getMtlsSecretIdTemplate() {
        return Labels.MTLS_IDENTITY_REPO;
    }
}
