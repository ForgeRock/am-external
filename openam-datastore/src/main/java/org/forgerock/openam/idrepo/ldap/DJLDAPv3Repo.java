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
 * Copyright 2013-2022 ForgeRock AS.
 * Portions Copyright 2016 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2016 Agile Digital Engineering.
 */
package org.forgerock.openam.idrepo.ldap;

import static com.sun.identity.idm.IdConstants.ID;
import static com.sun.identity.idm.IdConstants.ID_GENERATED;
import static com.sun.identity.idm.IdConstants.REV;
import static com.sun.identity.idm.IdConstants.USERNAME;
import static com.sun.identity.idm.IdRepoErrorCode.IDENTITY_ATTRIBUTE_INVALID;
import static com.sun.identity.idm.IdType.USER;
import static com.sun.identity.shared.Constants.ADMIN_PASSWORD_CHANGE_REQUEST_ATTR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.forgerock.openam.ldap.LDAPConstants.AD_LDAP_MATCHING_RULE_IN_CHAIN_OID;
import static org.forgerock.openam.ldap.LDAPConstants.AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.AD_UNICODE_PWD_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.BEHERA_SUPPORT_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.DEFAULT_USER_STATUS_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.DN_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.IDENTITIES_ARE_ENRICHED_AS_OAUTH2CLIENTS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ADAM_TYPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_AD_TYPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_MODE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_MODE_LDAPS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_MODE_STARTTLS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_POOL_MAX_SIZE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CONNECTION_POOL_MIN_SIZE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_CREATION_ATTR_MAPPING;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_DEFAULT_GROUP_MEMBER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_DNCACHE_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_DNCACHE_SIZE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_FILTERED_ROLE_ATTRS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_FILTERED_ROLE_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_FILTERED_ROLE_OBJECT_CLASS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_FILTERED_ROLE_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_ATTRS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_CONTAINER_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_CONTAINER_VALUE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_OBJECT_CLASS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_GROUP_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_MAX_RESULTS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_MEMBER_OF;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_MEMBER_URL;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PEOPLE_CONTAINER_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PEOPLE_CONTAINER_VALUE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_BASE_DN;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_SCOPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PROXIED_AUTHORIZATION_DENIED_FALLBACK;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PROXIED_AUTHORIZATION_ENABLED;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_ATTRS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_DN_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_FILTER_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_OBJECT_CLASS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_ROLE_SEARCH_SCOPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SEARCH_SCOPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_HEARTBEAT_INTERVAL;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_HEARTBEAT_TIME_UNIT;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_KEEPALIVE_SEARCH_BASE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_KEEPALIVE_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_LIST;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_PASSWORD;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_ROOT_SUFFIX;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVER_USER_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SERVICE_ATTRS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_STATUS_ACTIVE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_STATUS_INACTIVE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_SUPPORTED_TYPES_AND_OPERATIONS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_TIME_LIMIT;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_UNIQUE_MEMBER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_ATTRS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_OBJECT_CLASS;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_SEARCH_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_STATUS_ATTR_NAME;
import static org.forgerock.openam.ldap.LDAPConstants.OBJECT_CLASS_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.ROLE_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.ROLE_DN_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.ROLE_FILTER_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.STATUS_ACTIVE;
import static org.forgerock.openam.ldap.LDAPConstants.STATUS_INACTIVE;
import static org.forgerock.openam.ldap.LDAPConstants.UNIQUE_MEMBER_ATTR;
import static org.forgerock.openam.ldap.LDAPKeepAlive.DEFAULT_KEEP_ALIVE_INTERVAL;
import static org.forgerock.openam.ldap.LDAPKeepAlive.DEFAULT_KEEP_ALIVE_SEARCH_BASE;
import static org.forgerock.openam.ldap.LDAPKeepAlive.DEFAULT_KEEP_ALIVE_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPKeepAlive.DEFAULT_KEEP_ALIVE_TIMEOUT;
import static org.forgerock.openam.ldap.LDAPKeepAlive.configureKeepAliveAndAvailabilityCheck;
import static org.forgerock.openam.ldap.LDAPKeepAlive.configureRequestTimeout;
import static org.forgerock.openam.ldap.LDAPUtils.AFFINITY_ENABLED;
import static org.forgerock.openam.ldap.LDAPUtils.CACHED_POOL_OPTIONS;
import static org.forgerock.openam.ldap.LDAPUtils.partiallyEscapeAssertionValue;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.opendj.ldap.LdapClients.LDAP_CLIENT_REQUEST_TIMEOUT;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.cts.api.DataLayerException;
import org.forgerock.openam.idrepo.ldap.helpers.ADAMHelper;
import org.forgerock.openam.idrepo.ldap.helpers.ADHelper;
import org.forgerock.openam.idrepo.ldap.helpers.DirectoryHelper;
import org.forgerock.openam.idrepo.ldap.psearch.DJLDAPv3PersistentSearch;
import org.forgerock.openam.ldap.LDAPConstants;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LdapFromJsonQueryFilterVisitor;
import org.forgerock.openam.service.datastore.DataStoreConfig;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Dn;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LdapUrl;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PasswordPolicyRequestControl;
import org.forgerock.opendj.ldap.controls.ProxiedAuthV2RequestControl;
import org.forgerock.opendj.ldap.messages.BindRequest;
import org.forgerock.opendj.ldap.messages.ModifyRequest;
import org.forgerock.opendj.ldap.messages.Request;
import org.forgerock.opendj.ldap.messages.Requests;
import org.forgerock.opendj.ldap.messages.SearchRequest;
import org.forgerock.opendj.ldap.messages.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.ObjectClassType;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.UnknownSchemaElementException;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.Strings;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.PasswordPolicyException;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.SchemaType;

/**
 * This is an IdRepo implementation that utilizes the LDAP protocol via OpenDJ LDAP SDK to access directory servers.
 */
public class DJLDAPv3Repo extends IdRepo implements IdentityMovedOrRenamedListener {

    private static final String CLASS_NAME = DJLDAPv3Repo.class.getName();
    private static final String DEBUG_CLASS_NAME = "DJLDAPv3Repo";
    private static final Logger DEBUG = LoggerFactory.getLogger(DJLDAPv3Repo.class);
    private static final String ETAG_SEARCH_ATTRIBUTE = "etag";

    /**
     * Maps psearchids to persistent search connections, so different datastore instances can share the same psearch
     * connection when appropriate.
     */
    private static final Map<String, DJLDAPv3PersistentSearch> PERSISTENT_SEARCH_MAP = new HashMap<>();
    private static final String AM_AUTH = "amAuth";
    private static final String PASSWORD_ATTR_NAME = "userPassword";
    private static final AttributeDescription PASSWORD_ATTR_DESC = AttributeDescription.valueOf(PASSWORD_ATTR_NAME);
    private static final Filter DEFAULT_ROLE_SEARCH_FILTER =
            Filter.valueOf("(&(objectclass=ldapsubentry)(objectclass=nsmanagedroledefinition))");
    private static final Filter DEFAULT_FILTERED_ROLE_SEARCH_FILTER =
            Filter.valueOf("(&(objectclass=ldapsubentry)(objectclass=nsfilteredroledefinition))");

    private Set<LDAPURL> ldapServers;
    private IdRepoListener idRepoListener;
    private Map<IdType, Set<IdOperation>> supportedTypesAndOperations;
    private Map<String, String> creationAttributeMapping;
    private int heartBeatInterval = 10;
    private String heartBeatTimeUnit;
    private String keepAliveSearchBase;
    private String keepAliveSearchFilter;
    private String rootSuffix;
    private String userStatusAttr;
    private boolean alwaysActive;
    private String activeValue;
    private String inactiveValue;
    private String userSearchAttr;
    private String userNamingAttr;
    private String groupNamingAttr;
    private String roleNamingAttr;
    private String filteredRoleNamingAttr;
    private Set<String> userObjectClasses;
    private Set<String> groupObjectClasses;
    private Set<String> roleObjectClasses;
    private Set<String> filteredRoleObjectClasses;
    private Set<String> userAttributesAllowed;
    private Set<String> groupAttributesAllowed;
    private Set<String> roleAttributesAllowed;
    private Set<String> filteredRoleAttributesAllowed;
    private String memberOfAttr;
    private String uniqueMemberAttr;
    private String memberURLAttr;
    private String defaultGroupMember;
    private Filter userSearchFilter;
    private Filter groupSearchFilter;
    private Filter roleSearchFilter;
    private Filter filteredRoleSearchFilter;
    private String peopleContainerName;
    private String peopleContainerValue;
    private String groupContainerName;
    private String groupContainerValue;
    private String roleAttr;
    private String roleDNAttr;
    private String roleFilterAttr;
    private SearchScope defaultScope;
    private SearchScope roleScope;
    private int defaultSizeLimit;
    private int defaultTimeLimit;
    private DirectoryHelper helper;
    //although there is a max pool size, we are currently trebling that in order to be able to authenticate users
    private ConnectionFactory<Connection> connectionFactory;
    private ConnectionFactory<Connection> bindConnectionFactory;
    private ConnectionFactory<Connection> passwordChangeConnectionFactory;
    //holds service attributes for the current realm
    private Map<String, Map<String, Set<String>>> serviceMap;
    //holds the directory schema
    private volatile Schema schema;
    //provides a cache for DNs (if enabled), because an entry tends to be requested in bursts.
    private Cache<String, Dn> dnCache;
    // provides a switch to enable/disable the dnCache
    private boolean dnCacheEnabled;

    private boolean isSecure;
    private boolean useStartTLS;
    private boolean beheraSupportEnabled;
    private boolean adRecursiveGroupMembershipsEnabled;
    private boolean iotIdentitiesEnrichedAsOAuth2Client;
    private boolean proxiedAuthorizationEnabled;
    private boolean proxiedAuthorizationFallbackOnDenied;
    private boolean affinityEnabled;

    /**
     * Initializes the IdRepo instance, basically within this method we process
     * the configuration settings and set up the connection factories that will
     * be used later in the lifetime of the IdRepo plugin.
     *
     * @param configParams The IdRepo configuration as defined in the service
     * configurations.
     * @throws IdRepoException Shouldn't be thrown.
     */
    @Override
    public void initialize(Map<String, Set<String>> configParams) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("initialize invoked");
        }
        super.initialize(configParams);

        dnCacheEnabled = CollectionHelper.getBooleanMapAttr(configMap, LDAP_DNCACHE_ENABLED, true);
        if (dnCacheEnabled) {
            int cacheSize = CollectionHelper.getIntMapAttr(configParams, LDAP_DNCACHE_SIZE, 1500, DEBUG);
            CacheBuilder cacheBuilder = CacheBuilder.newBuilder().maximumSize(cacheSize);

            // As per Guava Cache JavaDoc, when the value for expireAfterAccess is set to zero the cache will
            // be effectively disabled. Therefore, AM will only use the expiry time when the value is greater
            // than zero.
            int dnCacheExpiryTime = SystemProperties.getAsInt(Constants.DN_CACHE_EXPIRE_TIME, 0);
            if (dnCacheExpiryTime > 0) {
                cacheBuilder.expireAfterAccess(dnCacheExpiryTime, TimeUnit.MILLISECONDS);
            }
            dnCache = cacheBuilder.build();
        }

        ldapServers = IdRepoUtils.getPrioritizedLDAPUrls(configParams.get(LDAP_SERVER_LIST));

        defaultSizeLimit = CollectionHelper.getIntMapAttr(configParams, LDAP_MAX_RESULTS, 100, DEBUG);
        defaultTimeLimit = CollectionHelper.getIntMapAttr(configParams, LDAP_TIME_LIMIT, 5, DEBUG);
        int maxPoolSize = CollectionHelper.getIntMapAttr(configParams, LDAP_CONNECTION_POOL_MAX_SIZE, 10, DEBUG);
        int minPoolSize = CollectionHelper.getIntMapAttr(configParams, LDAP_CONNECTION_POOL_MIN_SIZE, 1, DEBUG);
        affinityEnabled = CollectionHelper.getBooleanMapAttr(configMap, LDAPConstants.LDAP_CONNECTION_AFFINITY_ENABLED,
            false);

        String username = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_USER_NAME);
        char[] password = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_PASSWORD, "").toCharArray();
        proxiedAuthorizationEnabled = CollectionHelper.getBooleanMapAttr(configMap, LDAP_PROXIED_AUTHORIZATION_ENABLED,
                false);
        proxiedAuthorizationFallbackOnDenied = CollectionHelper.getBooleanMapAttr(configMap,
                LDAP_PROXIED_AUTHORIZATION_DENIED_FALLBACK, false);
        heartBeatInterval = CollectionHelper.getIntMapAttr(configParams, LDAP_SERVER_HEARTBEAT_INTERVAL,
                DEFAULT_KEEP_ALIVE_INTERVAL, DEBUG);

        heartBeatTimeUnit = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_HEARTBEAT_TIME_UNIT, "SECONDS");

        keepAliveSearchBase = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_KEEPALIVE_SEARCH_BASE,
                DEFAULT_KEEP_ALIVE_SEARCH_BASE);
        keepAliveSearchFilter = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_KEEPALIVE_SEARCH_FILTER,
                DEFAULT_KEEP_ALIVE_SEARCH_FILTER);

        String connectionMode = CollectionHelper.getMapAttr(configParams, LDAP_CONNECTION_MODE);
        useStartTLS = LDAP_CONNECTION_MODE_STARTTLS.equalsIgnoreCase(connectionMode);
        isSecure = LDAP_CONNECTION_MODE_LDAPS.equalsIgnoreCase(connectionMode);
        beheraSupportEnabled = CollectionHelper.getBooleanMapAttr(configMap, BEHERA_SUPPORT_ENABLED, false);
        iotIdentitiesEnrichedAsOAuth2Client = CollectionHelper.getBooleanMapAttr(configMap,
                IDENTITIES_ARE_ENRICHED_AS_OAUTH2CLIENTS, false);
        bindConnectionFactory = createConnectionFactory(null, null, minPoolSize, maxPoolSize);
        connectionFactory = createConnectionFactory(username, password, minPoolSize, maxPoolSize);
        passwordChangeConnectionFactory = createPasswordConnectionFactory(null, null, maxPoolSize);

        supportedTypesAndOperations =
                IdRepoUtils.parseSupportedTypesAndOperations(configParams.get(LDAP_SUPPORTED_TYPES_AND_OPERATIONS));
        userStatusAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_STATUS_ATTR_NAME);
        if (userStatusAttr == null || userStatusAttr.isEmpty()) {
            alwaysActive = true;
            userStatusAttr = DEFAULT_USER_STATUS_ATTR;
        }
        activeValue = CollectionHelper.getMapAttr(configParams, LDAP_STATUS_ACTIVE, STATUS_ACTIVE);
        inactiveValue = CollectionHelper.getMapAttr(configParams, LDAP_STATUS_INACTIVE, STATUS_INACTIVE);
        creationAttributeMapping = IdRepoUtils.parseAttributeMapping(configParams.get(LDAP_CREATION_ATTR_MAPPING));
        userNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_NAMING_ATTR);
        groupNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_NAMING_ATTR);
        roleNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_NAMING_ATTR);
        filteredRoleNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_FILTERED_ROLE_NAMING_ATTR);
        userSearchAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_SEARCH_ATTR);
        userAttributesAllowed = new CaseInsensitiveHashSet<>();
        Set<String> allowAttrs = configParams.get(LDAP_USER_ATTRS);
        if (allowAttrs != null) {
            userAttributesAllowed.addAll(allowAttrs);
        }
        groupAttributesAllowed = new CaseInsensitiveHashSet<>();
        allowAttrs = configParams.get(LDAP_GROUP_ATTRS);
        if (allowAttrs != null) {
            groupAttributesAllowed.addAll(allowAttrs);
        }
        roleAttributesAllowed = new CaseInsensitiveHashSet<>();
        allowAttrs = configParams.get(LDAP_ROLE_ATTRS);
        if (allowAttrs != null) {
            roleAttributesAllowed.addAll(allowAttrs);
        }
        filteredRoleAttributesAllowed = new CaseInsensitiveHashSet<>();
        allowAttrs = configParams.get(LDAP_FILTERED_ROLE_ATTRS);
        if (allowAttrs != null) {
            filteredRoleAttributesAllowed.addAll(allowAttrs);
        }
        userObjectClasses = getNonNullSettingValues(LDAP_USER_OBJECT_CLASS);
        groupObjectClasses = getNonNullSettingValues(LDAP_GROUP_OBJECT_CLASS);
        roleObjectClasses = getNonNullSettingValues(LDAP_ROLE_OBJECT_CLASS);
        filteredRoleObjectClasses = getNonNullSettingValues(LDAP_FILTERED_ROLE_OBJECT_CLASS);
        defaultGroupMember = CollectionHelper.getMapAttr(configParams, LDAP_DEFAULT_GROUP_MEMBER);
        uniqueMemberAttr = CollectionHelper.getMapAttr(configParams, LDAP_UNIQUE_MEMBER, UNIQUE_MEMBER_ATTR);
        memberURLAttr = CollectionHelper.getMapAttr(configParams, LDAP_MEMBER_URL);
        memberOfAttr = CollectionHelper.getMapAttr(configParams, LDAP_MEMBER_OF);
        peopleContainerName = CollectionHelper.getMapAttr(configParams, LDAP_PEOPLE_CONTAINER_NAME);
        peopleContainerValue = CollectionHelper.getMapAttr(configParams, LDAP_PEOPLE_CONTAINER_VALUE);
        groupContainerName = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_CONTAINER_NAME);
        groupContainerValue = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_CONTAINER_VALUE);
        roleAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_ATTR, ROLE_ATTR);
        roleDNAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_DN_ATTR, ROLE_DN_ATTR);
        roleFilterAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_FILTER_ATTR, ROLE_FILTER_ATTR);
        rootSuffix = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_ROOT_SUFFIX);
        userSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_USER_SEARCH_FILTER), Filter.objectClassPresent());
        groupSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_GROUP_SEARCH_FILTER), Filter.objectClassPresent());
        roleSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_ROLE_SEARCH_FILTER), DEFAULT_ROLE_SEARCH_FILTER);
        filteredRoleSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_FILTERED_ROLE_SEARCH_FILTER),
                DEFAULT_FILTERED_ROLE_SEARCH_FILTER);
        String serviceInfo = CollectionHelper.getMapAttr(configParams, LDAP_SERVICE_ATTRS);
        serviceMap = new HashMap<String, Map<String, Set<String>>>(new SOAPClient("dummy").decodeMap(serviceInfo));
        defaultScope = LDAPUtils.getSearchScope(
                CollectionHelper.getMapAttr(configParams, LDAP_SEARCH_SCOPE), SearchScope.WHOLE_SUBTREE);
        roleScope = LDAPUtils.getSearchScope(
                CollectionHelper.getMapAttr(configParams, LDAP_ROLE_SEARCH_SCOPE), SearchScope.WHOLE_SUBTREE);
        adRecursiveGroupMembershipsEnabled = CollectionHelper.getBooleanMapAttr(configMap,
            AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED, false);
        if (configParams.containsKey(LDAP_ADAM_TYPE)) {
            helper = new ADAMHelper();
        } else if (configParams.containsKey(LDAP_AD_TYPE)) {
            helper = new ADHelper();
        } else {
            helper = new DirectoryHelper();
        }

        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("IdRepo configuration:\n"
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(configMap, asSet(LDAP_SERVER_PASSWORD)));
        }
    }

    protected ConnectionFactory<Connection> createConnectionFactory(String username, char[] password, int minPoolSize,
        int maxPoolSize) {
        int idleTimeout = LdapConnectionFactoryProvider.getIdleConnectionTime();

        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("LDAP Heartbeat Timeout System Property: "
                    + SystemProperties.get(Constants.LDAP_HEARTBEAT_TIMEOUT));
        }

        final int heartBeatTimeout =
                SystemProperties.getAsInt(Constants.LDAP_HEARTBEAT_TIMEOUT, DEFAULT_KEEP_ALIVE_TIMEOUT);

        Options ldapOptions = Options.defaultOptions()
            .set(CACHED_POOL_OPTIONS, new LDAPUtils.CachedPoolOptions(minPoolSize, maxPoolSize, idleTimeout, SECONDS))
            .set(AFFINITY_ENABLED, affinityEnabled);

        ldapOptions = configureRequestTimeout(ldapOptions, defaultTimeLimit, SECONDS);
        ldapOptions = configureKeepAliveAndAvailabilityCheck(ldapOptions, heartBeatInterval, heartBeatTimeout, SECONDS,
                keepAliveSearchBase.trim(), keepAliveSearchFilter);

        DataStoreConfig config = DataStoreConfig.builder(null)
                .withLDAPURLs(LDAPUtils.getLdapUrls(ldapServers, isSecure))
                .withBindDN(username)
                .withBindPassword(password)
                .withMinimumConnectionPool(maxPoolSize)
                .withMaximumConnectionPool(maxPoolSize)
                .withUseSsl(isSecure)
                .withUseStartTLS(useStartTLS)
                .withAffinityEnabled(affinityEnabled)
                .build();

        if (maxPoolSize == 1) {
            return LdapConnectionFactoryProvider.wrapExistingConnectionFactory(
                    LDAPUtils.newFailoverConnectionFactory(
                            LDAPUtils.getLdapUrls(ldapServers, isSecure), username, password, heartBeatInterval,
                    heartBeatTimeUnit, useStartTLS, false, ldapOptions), config);
        } else {
            return LdapConnectionFactoryProvider.wrapExistingConnectionFactory(
                    LDAPUtils.newFailoverConnectionPool(
                            LDAPUtils.getLdapUrls(ldapServers, isSecure), username, password, maxPoolSize,
                            heartBeatInterval, heartBeatTimeUnit, useStartTLS, false, ldapOptions), config);
        }
    }


    protected ConnectionFactory<Connection> createPasswordConnectionFactory(String username, char[] password,
                                                                            int maxPoolSize) {

        Options ldapOptions = Options.defaultOptions()
                .set(LDAP_CLIENT_REQUEST_TIMEOUT, Duration.duration(defaultTimeLimit, SECONDS));

        final int heartBeatTimeout =
                SystemProperties.getAsInt(Constants.LDAP_HEARTBEAT_TIMEOUT, DEFAULT_KEEP_ALIVE_TIMEOUT);
        ldapOptions = configureKeepAliveAndAvailabilityCheck(ldapOptions, heartBeatInterval, heartBeatTimeout, SECONDS,
                keepAliveSearchBase.trim(), keepAliveSearchFilter);

        DataStoreConfig config = DataStoreConfig.builder(null)
                .withLDAPURLs(LDAPUtils.getLdapUrls(ldapServers, isSecure))
                .withBindDN(username)
                .withBindPassword(password)
                .withMinimumConnectionPool(maxPoolSize)
                .withMaximumConnectionPool(maxPoolSize)
                .withUseSsl(isSecure)
                .withUseStartTLS(useStartTLS)
                .build();

        return LdapConnectionFactoryProvider.wrapExistingConnectionFactory(
                LDAPUtils.newPasswordConnectionFactory(
                        LDAPUtils.getLdapUrls(ldapServers, isSecure), username, password, maxPoolSize,
                        heartBeatInterval, heartBeatTimeUnit, useStartTLS, false, ldapOptions), config);
    }

    /**
     * Tells whether this identity repository supports authentication.
     *
     * @return <code>true</code> since this repository supports authentication.
     */
    @Override
    public boolean supportsAuthentication() {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("supportsAuthentication invoked");
        }
        return true;
    }

    /**
     * Tries to bind as the user with the credentials passed in via callbacks. This authentication mechanism does not
     * handle password policies, nor password expiration.
     *
     * @param credentials The username/password combination.
     * @return <code>true</code> if the bind operation was successful.
     * @throws IdRepoException If the passed in username/password was null, or if the specified user cannot be found.
     * @throws AuthLoginException If an LDAP error occurs during authentication.
     * @throws InvalidPasswordException If the provided password is not valid, so Account Lockout can be triggered.
     */
    @Override
    public boolean authenticate(Callback[] credentials) throws IdRepoException, AuthLoginException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("authenticate invoked");
        }
        String userName = null;
        char[] password = null;
        for (Callback callback : credentials) {
            if (callback instanceof NameCallback) {
                userName = ((NameCallback) callback).getName();
            } else if (callback instanceof PasswordCallback) {
                password = ((PasswordCallback) callback).getPassword();
            }
        }
        if (userName == null || password == null) {
            throw newIdRepoException(IdRepoErrorCode.UNABLE_TO_AUTHENTICATE, CLASS_NAME);
        }
        Dn dn = findDNForAuth(USER, userName);
        BindRequest bindRequest = LDAPRequests.newSimpleBindRequest(dn, password);
        try (Connection conn = createBindConnection()) {
            return conn.bind(bindRequest).isSuccess();
        } catch (LdapException ere) {
            ResultCode resultCode = ere.getResult().getResultCode();
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("An error occurred while trying to authenticate a user: " + ere);
            }
            if (resultCode.equals(ResultCode.INVALID_CREDENTIALS)) {
                throw new InvalidPasswordException(AM_AUTH, "InvalidUP", null, userName, null);
            } else if (resultCode.equals(ResultCode.UNWILLING_TO_PERFORM)
                    || resultCode.equals(ResultCode.CONSTRAINT_VIOLATION)) {
                throw new AuthLoginException(AM_AUTH, "FAuth", null);
            } else if (resultCode.equals(ResultCode.INAPPROPRIATE_AUTHENTICATION)) {
                throw new AuthLoginException(AM_AUTH, "InappAuth", null);
            } else {
                throw new AuthLoginException(AM_AUTH, "LDAPex", null);
            }
        }
    }

    /**
     * Changes password for the given identity by binding as the user first (i.e. this is not password reset). In case
     * of Active Directory the password will be encoded first. This will issue a DELETE for the old password and an ADD
     * for the new password value.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param attrName The name of the password attribute, usually "userpassword" or "unicodepwd".
     * @param oldPassword The current password of the identity.
     * @param newPassword The new password of the idenity.
     * @throws IdRepoException If the identity type is invalid, or the new password is same as the old password
     * or the entry cannot be found, or some other LDAP error
     * occurs while changing the password (like password policy related errors).
     */
    @Override
    public void changePassword(SSOToken token, IdType type, String name, String attrName, String oldPassword,
            String newPassword) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("changePassword invoked");
        }


        assertIdTypeIsUser(type);
        assertPasswordNotSameAsOld(oldPassword, newPassword);

        Dn dn = getDNSkipExistenceCheck(type, name);
        BindRequest bindRequest = addBeheraControl(LDAPRequests.newSimpleBindRequest(dn, oldPassword.toCharArray()));
        ModifyRequest modifyRequest = addBeheraControl(LDAPRequests.newModifyRequest(dn));

        byte[] encodedOldPwd = helper.encodePassword(oldPassword);
        byte[] encodedNewPwd = helper.encodePassword(newPassword);

        modifyRequest.addModification(ModificationType.DELETE, attrName, encodedOldPwd);
        modifyRequest.addModification(ModificationType.ADD, attrName, encodedNewPwd);

        if (iotIdentitiesEnrichedAsOAuth2Client) {
            changeSunKeyValueUserPassword(dn, oldPassword, newPassword);
        }

        try (Connection conn = createPasswordConnection()) {
            conn.bind(bindRequest);
            conn.modify(modifyRequest);
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to change password for identity: " + name, ere);
            try {
                handleErrorResult(ere);
            } catch (IdRepoException e) {
                throw new PasswordPolicyException(e);
            }
        }
    }

    private void assertIdTypeIsUser(IdType type) throws IdRepoUnsupportedOpException {
        if (!type.equals(USER)) {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.CHANGE_PASSWORD_ONLY_FOR_USER, new Object[]{CLASS_NAME});
        }
    }

    private void assertPasswordNotSameAsOld(String oldPassword, String newPassword) throws PasswordPolicyException {
        if (oldPassword.equals(newPassword)) {
            throw new PasswordPolicyException(IdRepoErrorCode.PASSWORD_IN_HISTORY, new Object[]{CLASS_NAME});
        }
    }

    /**
     * Returns a fully qualified name of the identity, which should be unique per data store.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return Fully qualified name of this identity or <code>null</code> if the identity cannot be found.
     * @throws IdRepoException If there was an error while looking up the user.
     */
    @Override
    public String getFullyQualifiedName(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getFullyQualifiedName invoked");
        }
        try {
            return ldapServers + "/" + getDN(type, name);
        } catch (IdentityNotFoundException infe) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("Unable to find identity with name " + name + " and type " + type);
            }
            return null;
        }
    }

    /**
     * Returns DN under which the named identity may be stored by this {@link IdRepo}.
     * <p/>
     * Nb. This method does not verify that the identity is actually present.
     *
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return DN as reference to the identity within the data store.
     * @throws IdRepoException If there was an error while generating the DN.
     */
    @Override
    public Optional<String> getObjectId(IdType type, String name) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getObjectId invoked");
        }
        try {
            return Optional.ofNullable(generateDN(type, name).toString());
        } catch (IdentityNotFoundException infe) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("Unable to find identity with name " + name + " and type " + type);
            }
            return Optional.empty();
        }
    }

    /**
     * Returns the set of supported operations for a given identity type.
     *
     * @param type The identity type for which we want to get the supported operations.
     * @return The set of supported operations for this identity type.
     */
    @Override
    public Set<IdOperation> getSupportedOperations(IdType type) {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getSupportedOperations invoked");
        }
        return supportedTypesAndOperations.get(type);
    }

    /**
     * Returns the set of supported identity types.
     *
     * @return The set of supported identity types.
     */
    @Override
    public Set<IdType> getSupportedTypes() {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getSupportedTypes invoked");
        }
        return supportedTypesAndOperations.keySet();
    }

    /**
     * Sets the user status to the value provided for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param active The new status of the identity.
     * @throws IdRepoException If the identity type is invalid, or either the previous status retrieval failed (AD), or
     * there was a failure while setting the status.
     */
    @Override
    public void setActiveStatus(SSOToken token, IdType type, String name, boolean active)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("setActiveStatus invoked");
        }
        if (!type.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED, CLASS_NAME);
        }
        String status = helper.getStatus(this, name, active, userStatusAttr, activeValue, inactiveValue);
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("Setting user status to: " + status);
        }
        Map<String, Set<String>> attr = new HashMap<>(1);
        attr.put(userStatusAttr, asSet(status));
        setAttributes(token, type, name, attr, false);
    }

    /**
     * Tells whether the given identity is considered as "active" or not. In case the user status attribute is not
     * configured, this method will always return <code>true</code>. In case of Active Directory the returned
     * userAccountControl attribute will be masked with 0x2 to detect whether the given account is disabled or not.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @return <code>true</code> if user status attribute is not configured, or decision based on the status
     * attribute value. If there was any error while retrieving the status attribute this method will return
     * <code>false</code>.
     * @throws IdRepoException If the identity type is invalid.
     */
    @Override
    public boolean isActive(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("isActive invoked");
        }
        if (!type.equals(USER)) {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                    new Object[]{CLASS_NAME, IdOperation.READ.getName(), type.getName()});
        }
        // check if the identity exists, using getDN here on purpose instead of using isExists
        // to trigger an exception if the identity does not exist
        getDN(type, name);
        if (alwaysActive) {
            return true;
        }
        Map<String, Set<String>> attrMap;
        try {
            attrMap = getAttributes(token, type, name, asSet(userStatusAttr));
        } catch (IdRepoException ire) {
            return false;
        }
        String status = CollectionHelper.getMapAttr(attrMap, userStatusAttr);
        return status == null || helper.isActive(status, activeValue);
    }

    /**
     * Tells whether a given identity exists or not.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return <code>true</code> if the identity exists.
     * @throws IdRepoException Shouldn't be thrown.
     */
    @Override
    public boolean isExists(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("isExists invoked");
        }
        try {
            //get the user DN, if there is no such entry, this will already fail.
            getDN(type, name);
        } catch (IdentityNotFoundException infe) {
            return false;
        }
        return true;
    }

    /**
     * Creates a new identity using the passed in attributes. The following steps will be performed with the passed in
     * data:
     * <ul>
     *  <li>The password will be encoded in case we are dealing with AD.</li>
     *  <li>If the attribute map contains the default status attribute, then it will be converted to the status values
     *      specified in the configuration.</li>
     *  <li>Performing creation attribute mapping, so certain attributes can have default values (coming from other
     *      attributes, or from the identity name if there is no mapping for the attribute).</li>
     *  <li>Removes all attributes that are not defined in the configuration.</li>
     * </ul>
     * If the default group member setting is being used and a new group identity is being created, the newly created
     * group will also have the default group member assigned.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrMap The attributes of the new identity, that needs to be stored.
     * @return The DN of the newly created identity
     * @throws IdRepoException If there is an error while creating the new identity, or if it's a group and there is a
     * problem while adding the default group member.
     */
    @Override
    public String create(SSOToken token, IdType type, String name, Map<String, Set<String>> attrMap)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("Create invoked on " + type + ": " + name + " attrMap = "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap, null));
        }

        Dn dn;
        String exposedId;

        if (type.equals(USER)) {
            exposedId = generateIdForUser(attrMap, name);
            dn = generateDN(USER, exposedId);
        } else {
            dn = generateDN(type, name);
            exposedId = name;
        }

        Set<String> objectClasses = getObjectClasses(type);

        //First we should make sure that we wrap the attributes with a case insensitive hashmap.
        attrMap = new CaseInsensitiveHashMap<>(attrMap);
        String userPassword = Optional
                .ofNullable(attrMap.get(PASSWORD_ATTR_NAME))
                .orElseGet(Collections::emptySet)
                .stream()
                .findFirst()
                .orElse("");
        byte[] encodedPwd = helper.encodePassword(type, attrMap.get(AD_UNICODE_PWD_ATTR));

        //Let's set the userstatus as it is configured in the datastore.
        mapUserStatus(type, attrMap);
        //In case some attributes are missing use the create attribute mapping to get those values.
        mapCreationAttributes(type, name, attrMap);

        //ensure we convert from _id to the search attribute name prior to purging unknown attributes
        if (type.equals(USER)) {
            updateSearchAndNamingAttributesInAttributeMapForUser(attrMap, name);
        }

        //and lastly we should make sure that we get rid of the attributes that are not known by the datastore.
        attrMap = removeUndefinedAttributes(type, attrMap);

        Set<String> ocs = attrMap.get(OBJECT_CLASS_ATTR);
        if (ocs != null) {
            ocs.addAll(objectClasses);
        } else {
            attrMap.put(OBJECT_CLASS_ATTR, objectClasses);
        }

        Entry entry = new LinkedHashMapEntry(dn);
        Set<String> attributeValue;
        for (Map.Entry<String, Set<String>> attr : attrMap.entrySet()) {
            // Add only attributes whose values are not empty or null
            attributeValue = attr.getValue();
            if (attributeValue != null  && !attributeValue.isEmpty()) {
                entry.addAttribute(attr.getKey(), attributeValue.toArray());
            }
        }

        if (type.equals(IdType.GROUP) && defaultGroupMember != null) {
            entry.addAttribute(uniqueMemberAttr, defaultGroupMember);
        }

        if (encodedPwd != null) {
            entry.replaceAttribute(AD_UNICODE_PWD_ATTR, encodedPwd);
        }

        try (Connection conn = createConnection()) {
            conn.add(addBeheraControl(LDAPRequests.newAddRequest(entry)));
            if (dnCacheEnabled) {
                dnCache.put(generateDNCacheKey(name, type), dn);
            }
            if (type.equals(IdType.GROUP) && defaultGroupMember != null && memberOfAttr != null) {
                conn.modify(LDAPRequests.newModifyRequest(defaultGroupMember)
                                        .addModification(ModificationType.ADD, memberOfAttr, dn));
            }
        } catch (LdapException ere) {
            DEBUG.error("Unable to add a new entry: {}", name, ere);
            if (ResultCode.ENTRY_ALREADY_EXISTS.equals(ere.getResult().getResultCode())) {
                throw IdRepoDuplicateObjectException.nameAlreadyExists(name);
            }
            handleErrorResult(ere);
        }

        if (iotIdentitiesEnrichedAsOAuth2Client) {
            changeSunKeyValueUserPassword(dn, null, userPassword);
            changeSunKeyValueUserStatus(dn, null, activeValue);
        }

        return exposedId;
    }

    private String generateIdForUser(Map<String, Set<String>> attrMap, String name) throws IdRepoException {

        // if our search and naming attributes are the same, but the values provided for the user's
        // id and username are different, then we must error out -- UNLESS the id was generated by the
        // resource endpoint (e.g. it was given a UUID) in which case continue
        final boolean searchAndNamingAttributesEqual = userSearchAttr.equals(userNamingAttr);
        final Set<String> id = attrMap.get(ID);
        final Set<String> userNames = attrMap.get(USERNAME);
        final boolean idWasGenerated = attrMap.containsKey(ID_GENERATED);
        if (searchAndNamingAttributesEqual
                && CollectionUtils.isNotEmpty(id)
                && CollectionUtils.isNotEmpty(userNames)
                && !idWasGenerated //if we generated the _id, do not enforce as we will ignore
                && !id.iterator().next().equals(userNames.iterator().next())) {
            throw new IdRepoException("id in path does not match id in request body", IDENTITY_ATTRIBUTE_INVALID);
        }

        // if we were generated, and the search and naming attributes are the same, place the username
        // into the attribute map in the _id field. The calling code will look at the _id field to determine the
        // resourceId against which to look up the user, so this ensures that we attempt to read using the username,
        // not the generated id which is essentially discarded.
        if (searchAndNamingAttributesEqual && idWasGenerated) {
            attrMap.put(ID, userNames);
        }

        // if our search and naming attributes aren't the same, user the value of the _id as the RDN component
        // if they are the same, use the _username as the RDN component
        // if we can't match any other case (e.g. we are coming through a version of the resource which does not
        // inject the _id or _username fields) use the passed in name as for any other object type
        if (CollectionUtils.isNotEmpty(id) && !searchAndNamingAttributesEqual) {
            return id.iterator().next();
        } else if (CollectionUtils.isNotEmpty(userNames)) {
            return userNames.iterator().next();
        } else {
            return name;
        }
    }

    private void updateSearchAndNamingAttributesInAttributeMapForUser(Map<String, Set<String>> attrMap, String name) {
        Set<String> userNames = attrMap.get(USERNAME);
        if (!userSearchAttr.equals(userNamingAttr)) {
            // use the _id and _username values respectively
            attrMap.put(getSearchAttribute(USER), attrMap.get(ID));
            attrMap.put(getNamingAttribute(USER), userNames);
        } else if (CollectionUtils.isEmpty(userNames)) {
            // default to just the passed-in name
            attrMap.put(getSearchAttribute(USER), asSet(name));
        } else {
            // use the _username
            attrMap.put(getSearchAttribute(USER), userNames);
        }
    }

    /**
     * Returns all the attributes that are defined in the configuration for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return The attributes of this identity.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, Set<String>> getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getAttributes invoked");
        }

        return getAttributes(token, type, name, null);
    }

    /**
     * Returns all the requested attributes that are defined in the configuration for this given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested attributes or <code>null</code> to retrieve all the attributes.
     * @return The requested attributes of this identity.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, Set<String>> getAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getAttributes2 invoked");
        }

        return getAttributes(type, name, attrNames, new StringAttributeExtractor());
    }

    /**
     * Returns all the requested binary attributes that are defined in the configuration for this given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested binary attributes or <code>null</code> to retrieve all the
     * attributes.
     * @return The requested attributes of this identity in binary format.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, byte[][]> getBinaryAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getBinaryAttributes invoked");
        }
        return getAttributes(type, name, attrNames, new BinaryAttributeExtractor());
    }

    /**
     * Returns all the requested attributes either in binary or in String format. Only the attributes defined in the
     * configuration will be returned for this given identity. In case the default "inetUserStatus" attribute has been
     * requested, it will be converted to the actual status attribute during query, and while processing it will be
     * mapped back to standard "inetUserStatus" values as well (rather than returning the configuration/directory
     * specific values). If there is an attempt to read a realm identity type's objectclass attribute, this method will
     * return an empty map right away (legacy handling). If the dn attribute has been requested, and it's also defined
     * in the configuration, then the attributemap will also contain the dn in the result.
     *
     * @param <T>
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested attributes or <code>null</code> to retrieve all the attributes.
     * @param function A function that can extract String or byte array values from an LDAP attribute.
     * @return The requested attributes in string or binary format.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    private <T> Map<String, T> getAttributes(IdType type, String name, Set<String> attrNames,
            Function<Attribute, T, IdRepoException> function) throws IdRepoException {
        Set<String> attrs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (CollectionUtils.isNotEmpty(attrNames)) {
            attrs.addAll(attrNames);
        }

        if (type.equals(IdType.REALM) && attrs.contains(OBJECT_CLASS_ATTR)) {
            return new HashMap<>(0);
        }
        if (type.equals(USER) && attrs.contains(DEFAULT_USER_STATUS_ATTR)) {
            attrs.add(userStatusAttr);
        }

        Set<String> definedAttributes = getDefinedAttributes(type);
        if (attrs.isEmpty() || attrs.contains("*")) {
            attrs.clear();
            if (definedAttributes.isEmpty()) {
                attrs.add("*");
            } else {
                attrs.addAll(definedAttributes);
            }
        } else {
            if (!definedAttributes.isEmpty()) {
                attrs.retainAll(definedAttributes);
            }
            if (attrs.isEmpty()) {
                //there were only non-defined attributes requested, so we shouldn't return anything here.
                // getDN() will make sure the entry is effectively existing.
                getDN(type, name);
                return new HashMap<>(0);
            }
        }

        if (type.equals(USER)) {
            attrs.add(ETAG_SEARCH_ATTRIBUTE);
        }

        final Map<String, T> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Dn dn = getDNSkipExistenceCheck(type, name);
        try (Connection conn = createConnection()) {
            SearchResultEntry entry = conn.searchSingleEntry(
                    LDAPRequests.newSingleEntrySearchRequest(dn, attrs.toArray(new String[attrs.size()])));
            for (Attribute attribute : entry.getAllAttributes()) {
                String attrName = attribute.getAttributeDescriptionAsString();
                if (!definedAttributes.isEmpty() && !definedAttributes.contains(attrName)) {
                    continue;
                }
                result.put(attribute.getAttributeDescriptionAsString(), function.apply(attribute));
                if (attrName.equalsIgnoreCase(userStatusAttr)) {
                    // Always include the DEFAULT_USER_STATUS_ATTR to cover any mapped isActive logic in envs like AD.
                    String converted = helper.convertToInetUserStatus(attribute.firstValueAsString(), activeValue);
                    result.put(DEFAULT_USER_STATUS_ATTR,
                            function.apply(new LinkedAttribute(DEFAULT_USER_STATUS_ATTR, converted)));
                }
                if (attrName.equalsIgnoreCase(userNamingAttr)) {
                    // Add _username as an alias for the authentication naming attribute to allow higher level code
                    // to access the username without needing to know which attribute this is stored in
                    result.put(USERNAME, function.apply(attribute));
                }
                if (attrName.equalsIgnoreCase(userSearchAttr)) {
                    // Add _id as an alias for the search attribute to allow higher level code
                    // to access the username without needing to know which attribute this is stored in
                    result.put(ID, function.apply(attribute));
                }
                if (attrName.equalsIgnoreCase(ETAG_SEARCH_ATTRIBUTE)) {
                    // Add _rev as an alias for the etag value to allow higher level code to access the revision
                    result.put(REV, function.apply(attribute));
                }
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while getting user attributes", ere);
            handleErrorResult(ere);
        }
        if (attrs.contains(DN_ATTR)) {
            result.put(DN_ATTR, function.apply(new LinkedAttribute(DN_ATTR, dn)));
        }

        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getAttributes returning attrMap: "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(result, null));
        }
        return result;
    }

    /**
     * Sets the provided attributes for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attributes The attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    @Override
    public void setAttributes(SSOToken token, IdType type, String name, Map<String, Set<String>> attributes,
            boolean isAdd) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("setAttributes invoked");
        }
        setAttributes(token, type, name, attributes, isAdd, true, true);
    }

    /**
     * Sets the provided binary attributes for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attributes The binary attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    @Override
    public void setBinaryAttributes(SSOToken token, IdType type, String name, Map<String, byte[][]> attributes,
            boolean isAdd) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("setBinaryAttributes invoked");
        }
        setAttributes(token, type, name, attributes, isAdd, false, true);
    }

    /**
     * Sets the provided attributes (string or binary) for the given identity. The following steps will be performed
     * prior to modification:
     * <ul>
     *  <li>The password will be encoded in case we are dealing with AD.</li>
     *  <li>Anything related to undefined attributes will be ignored.</li>
     *  <li>If the attribute map contains the default status attribute, then it will be converted to the status value
     *      specified in the configuration.</li>
     *  <li>In case changeOCs is set to <code>true</code>, the method will traverse through all the defined
     *      objectclasses to see if there is any attribute in the attributes map that is defined by that objectclass.
     *      These objectclasses will be collected and will be part of the modificationset with the other changes.</li>
     * </ul>
     * The attributes will be translated to modifications based on the followings:
     * <ul>
     *  <li>If the attribute has no values in the map, it will be considered as an attribute DELETE.</li>
     *  <li>In any other case based on the value of isAdd parameter, it will be either ADD, or REPLACE.</li>
     * </ul>
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param inputAttributes The attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @param isString Whether the provided attributes are in string or binary format.
     * @param changeOCs Whether the module should adjust the objectclasses for the entry or not.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    private void setAttributes(SSOToken token, IdType type, String name, Map inputAttributes,
            boolean isAdd, boolean isString, boolean changeOCs) throws IdRepoException {
        final Dn userDN = getDNSkipExistenceCheck(type, name);
        ModifyRequest modifyRequest = addBeheraControl(LDAPRequests.newModifyRequest(userDN));

        // check if it's password change request by admin
        String adminPasswordChangeAttr = SystemProperties.get(ADMIN_PASSWORD_CHANGE_REQUEST_ATTR);
        final boolean isAdminPasswordChangeRequest = StringUtils.isNotEmpty(adminPasswordChangeAttr)
                && inputAttributes.containsKey(adminPasswordChangeAttr);

        Map<String, Object> attributes = removeUndefinedAttributes(type, inputAttributes);

        if (type.equals(USER)) {
            if (userSearchAttr.equals(userNamingAttr)
                    && attributes.containsKey(userSearchAttr)
                    && !((Set<String>) attributes.get(userSearchAttr)).iterator().next().equals(name)) {
                throw new IdRepoException("id in path does not match id in request body", IDENTITY_ATTRIBUTE_INVALID);
            }

            Object newStatus = attributes.get(DEFAULT_USER_STATUS_ATTR);
            if (newStatus != null) {
                String newStatusValue = null;
                if (newStatus instanceof Set) {
                    newStatusValue = ((Set<String>) newStatus).iterator().next();
                } else if (newStatus instanceof byte[][]) {
                    newStatusValue = new String(((byte[][]) newStatus)[0], Charset.forName("UTF-8"));
                }
                newStatusValue = helper.getStatus(this, name, !STATUS_INACTIVE.equals(newStatusValue),
                        userStatusAttr, activeValue, inactiveValue);

                if (!attributes.containsKey(userStatusAttr)) {
                    attributes.remove(DEFAULT_USER_STATUS_ATTR);
                    if (isString) {
                        attributes.put(userStatusAttr, asSet(newStatusValue));
                    } else {
                        byte[][] binValue = new byte[1][];
                        binValue[0] = newStatusValue.getBytes(Charset.forName("UTF-8"));
                        attributes.put(userStatusAttr, binValue);
                    }
                }

                if (iotIdentitiesEnrichedAsOAuth2Client) {
                    String oldStatusValue = activeValue.equals(newStatusValue) ? inactiveValue : activeValue;
                    changeSunKeyValueUserStatus(userDN, oldStatusValue, newStatusValue);
                }
            }
        }

        for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) attributes.entrySet()) {
            Object values = entry.getValue();
            String attrName = entry.getKey();
            Attribute attr = new LinkedAttribute(attrName);
            if (AD_UNICODE_PWD_ATTR.equalsIgnoreCase(attrName)) {
                if (values instanceof byte[][]) {
                    attr.add(ByteString.valueOfBytes(helper.encodePassword(USER, (byte[][]) values)));
                } else {
                    attr.add(ByteString.valueOfBytes(helper.encodePassword(USER, (Set) values)));
                }
            } else if (values instanceof byte[][]) {
                for (byte[] bytes : (byte[][]) values) {
                    attr.add(ByteString.valueOfBytes(bytes));
                }
            } else if (values instanceof Set) {
                for (String value : (Set<String>) values) {
                    attr.add(value);
                }
            }
            if (attr.isEmpty()) {
                modifyRequest.addModification(new Modification(ModificationType.REPLACE, attr));
            } else {
                modifyRequest.addModification(
                        new Modification(isAdd ? ModificationType.ADD : ModificationType.REPLACE, attr));
            }
        }
        if (modifyRequest.getModifications().isEmpty()) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("setAttributes: there are no modifications to perform. {}: {}",
                        "Input attributes are either empty or not present in LDAP User attributes",
                        inputAttributes.keySet()
                                .stream()
                                .filter(Predicate.not(getDefinedAttributes(type)::contains))
                                .collect(Collectors.toSet()));
            }
            throw newIdRepoException(IdRepoErrorCode.ILLEGAL_ARGUMENTS);
        }
        if (type.equals(USER) && changeOCs) {
            Set<String> missingOCs = new CaseInsensitiveHashSet<>();
            Map<String, Set<String>> attrs = getAttributes(token, type, name, asSet(OBJECT_CLASS_ATTR));
            Set<String> ocs = attrs.get(OBJECT_CLASS_ATTR);

            //if the user has objectclasses that are defined in the config, but not defined in the entry then add those
            //to missingOCs
            if (ocs != null) {
                missingOCs.addAll(getObjectClasses(type));
                missingOCs.removeAll(ocs);
            }

            //if the missingOCs is not empty (i.e. there are objectclasses that are not present in the entry yet)
            if (!missingOCs.isEmpty()) {
                Object obj = attributes.get(OBJECT_CLASS_ATTR);
                //if the API user has also added some of his objectclasses, then let's remove those from missingOCs
                if (obj instanceof Set) {
                    missingOCs.removeAll((Set<String>) obj);
                }
                //for every single objectclass that needs to be added, let's check if they contain an attribute that we
                //wanted to add to the entry.
                Set<String> newOCs = new HashSet<>(2);
                Schema dirSchema = getSchema();
                for (String objectClass : missingOCs) {
                    try {
                        ObjectClass oc = dirSchema.getObjectClass(objectClass);
                        //we should never add new structural objectclasses, see RFC 4512
                        if (!oc.getObjectClassType().equals(ObjectClassType.STRUCTURAL)) {
                            for (String attrName : (Set<String>) attributes.keySet()) {
                                //before we start to add too many objectclasses here...
                                if (!attrName.equalsIgnoreCase(OBJECT_CLASS_ATTR)
                                        && oc.isRequiredOrOptional(dirSchema.getAttributeType(attrName))) {
                                    newOCs.add(objectClass);
                                    break;
                                }
                            }
                        }
                    } catch (UnknownSchemaElementException usee) {
                        if (DEBUG.isWarnEnabled()) {
                            DEBUG.warn("Unable to find a schema element: " + usee.getMessage());
                        }
                    }
                }
                missingOCs = newOCs;
                //it is possible that none of the missing objectclasses are actually covering any new attributes
                if (!missingOCs.isEmpty()) {
                    //based on these let's add the extra objectclasses to the modificationset
                    modifyRequest.addModification(new Modification(ModificationType.ADD,
                            new LinkedAttribute(OBJECT_CLASS_ATTR, missingOCs)));
                }
            }
        }

        try {
            updateAsProxiedAuthzIfNeeded(modifyRequest, userDN, isAdminPasswordChangeRequest);
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while setting attributes for identity: {}", name, ere);
            handleErrorResult(ere);
        }
    }

    private void updateAsProxiedAuthzIfNeeded(ModifyRequest request, Dn userDN, boolean isAdminPasswordChangeRequest)
            throws IdRepoException, LdapException {
        try (Connection conn = createConnection()) {
            if (proxiedAuthorizationEnabled && !isAdminPasswordChangeRequest) {
                for (ModifyRequest modifyRequest : segregateModifyRequest(request, userDN)) {
                    updateAsProxiedAuthzIfNeeded(conn, modifyRequest, userDN);
                }
            } else {
                conn.modify(request);
            }
        }
    }

    private void updateAsProxiedAuthzIfNeeded(Connection conn, ModifyRequest request, Dn userDN) throws LdapException {
        ModifyRequest origRequest = Requests.copyOfModifyRequest(request);
        boolean isProxiedOriginalRequest = adaptModifyRequest(request, userDN);
        try {
            conn.modify(request);
        } catch (LdapException ere) {
            ResultCode resultCode = ere.getResult().getResultCode();
            if (DEBUG.isDebugEnabled()) {
                String detail = request.getModifications().stream().map(m -> m.getAttribute()
                        .getAttributeDescription().toString()).collect(Collectors.joining(",", " [", "]"));
                DEBUG.debug("{}.{}: Update failed on {}{} : {}", DEBUG_CLASS_NAME, "updateAsProxiedAuthzIfNeeded",
                        userDN, detail, ere.getMessage());
            }
            if (isProxiedOriginalRequest && proxiedAuthorizationFallbackOnDenied) {
                if ((ResultCode.AUTHORIZATION_DENIED.equals(resultCode)
                        || ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(resultCode))) {
                    DEBUG.debug("{}.{}: Retrying without proxy-auth on error {}",
                            DEBUG_CLASS_NAME, "updateAsProxiedAuthzIfNeeded", resultCode);
                    conn.modify(origRequest);
                    return;
                }
            }
            DEBUG.debug("{}.{}: Proxy auth update not retried: proxiedAuthzFallbackOnDenied={}/{} resultCode={}",
                    DEBUG_CLASS_NAME, "updateAsProxiedAuthzIfNeeded", proxiedAuthorizationFallbackOnDenied,
                    isProxiedOriginalRequest, resultCode);
            throw ere;
        }
    }

    private List<ModifyRequest> segregateModifyRequest(ModifyRequest modifyRequest, Dn userDN) {
        List<Modification> nonPasswordModifications = new ArrayList<>();
        List<Modification> passwordModifications = new ArrayList<>();
        for (Modification modification : modifyRequest.getModifications()) {
            if (modification.getAttribute().getAttributeDescription().withoutAnyOptions().matches(PASSWORD_ATTR_DESC)) {
                passwordModifications.add(modification);
            } else {
                nonPasswordModifications.add(modification);
            }
        }
        if (!passwordModifications.isEmpty() && !nonPasswordModifications.isEmpty()) {
            List<ModifyRequest> modifyRequests = new ArrayList<>();
            for (List<Modification> m : Arrays.asList(nonPasswordModifications, passwordModifications)) {
                ModifyRequest request = LDAPRequests.newModifyRequest(userDN).addControls(modifyRequest.getControls());
                request.getModifications().addAll(m);
                modifyRequests.add(request);
            }
            DEBUG.debug("{}.{}: Modify request segregated into non-password and password : {} parts",
                    DEBUG_CLASS_NAME, "segregateModifyRequest", modifyRequests.size());
            return modifyRequests;
        } else {
            return Collections.singletonList(modifyRequest);
        }
    }

    /**
     * Returns a modified request if the request is adapted with some extra control
     * like proxied control if the request contains changes to password attribute.
     *
     * @param modifyRequest the modify request that will be modified in place
     * @param userDN the user DN for this request
     * @return true if the request is altered from original state
     */
    private boolean adaptModifyRequest(ModifyRequest modifyRequest, Dn userDN) {
        for (Modification modification : modifyRequest.getModifications()) {
            if (PASSWORD_ATTR_DESC.matches(modification.getAttribute().getAttributeDescription().withoutAnyOptions())) {
                String authzId = "dn:" + userDN;
                DEBUG.debug("{}.{}: Modify request will be performed by proxy user: {}",
                        DEBUG_CLASS_NAME, "adaptModifyRequest", authzId);
                modifyRequest.addControl(ProxiedAuthV2RequestControl.newControl(authzId));
                return true;
            }
        }
        return false;
    }

    /**
     * Adds Behera Control to the LDAPRequest.
     */
    private <T extends Request> T addBeheraControl(T request) {
        if (beheraSupportEnabled) {
            return (T) request.addControl(PasswordPolicyRequestControl.newControl(true));
        }
        return request;
    }

    private void changeSunKeyValueUserPassword(Dn userDN, String oldPassword, String newPassword)
            throws IdRepoException {
        ModifyRequest modifyRequest = addBeheraControl(LDAPRequests.newModifyRequest(userDN));

        if (!Strings.isBlank(oldPassword)) {
            modifyRequest.addModification(ModificationType.DELETE, "sunKeyValue",
                    "userpassword=" + Crypt.encode(oldPassword));
        }

        modifyRequest.addModification(ModificationType.ADD, "sunKeyValue",
                "userpassword=" + Crypt.encode(newPassword));

        if (proxiedAuthorizationEnabled) {
            adaptModifyRequest(modifyRequest, userDN);
        }

        try (Connection conn = createConnection()) {
            conn.modify(modifyRequest);
        } catch (LdapException ere) {
            DEBUG.error("An error while setting sunKeyValue userpassword for the identity: " + userDN, ere);
            handleErrorResult(ere);
        }
    }

    private void changeSunKeyValueUserStatus(Dn userDN, String oldStatus, String newStatus) throws IdRepoException {
        ModifyRequest modifyRequest = addBeheraControl(LDAPRequests.newModifyRequest(userDN));

        if (!Strings.isBlank(oldStatus)) {
            modifyRequest.addModification(ModificationType.DELETE, "sunKeyValue",
                    "sunIdentityServerDeviceStatus=" + oldStatus);
        }
        modifyRequest.addModification(ModificationType.ADD, "sunKeyValue",
                "sunIdentityServerDeviceStatus=" + newStatus);

        if (proxiedAuthorizationEnabled) {
            adaptModifyRequest(modifyRequest, userDN);
        }

        try (Connection conn = createConnection()) {
            conn.modify(modifyRequest);
        } catch (LdapException ere) {
            DEBUG.error("An error while setting sunKeyValue sunIdentityServerDeviceStatus for the identity: "
                    + userDN, ere);
            handleErrorResult(ere);
        }
    }

    /**
     * Removes the specified attributes from the identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The set of attribute names that needs to be removed from the identity.
     * @throws IdRepoException If there is no attribute name provided, or if the identity cannot be found, or there is
     * an error while modifying the entry.
     */
    @Override
    public void removeAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("removeAttributes invoked");
        }
        attrNames = removeUndefinedAttributes(type, attrNames);
        if (attrNames.isEmpty()) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("removeAttributes: there are no modifications to perform. {}: {}",
                        "Input attributes are either empty or not present in LDAP User attributes",
                        attrNames.stream()
                                .filter(Predicate.not(getDefinedAttributes(type)::contains))
                                .collect(Collectors.toSet()));
            }
            throw newIdRepoException(IdRepoErrorCode.ILLEGAL_ARGUMENTS);
        }
        Dn dn = getDNSkipExistenceCheck(type, name);
        ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(dn);
        for (String attr : attrNames) {
            modifyRequest.addModification(ModificationType.DELETE, attr);
        }
        try (Connection conn = createConnection()) {
            conn.modify(modifyRequest);
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while removing attributes from identity: " + name
                    + " attributes: " + attrNames, ere);
            handleErrorResult(ere);
        }
    }

    /**
     * Performs a search in the directory based on the provided parameters.
     * Using the pattern and avPairs parameters an example search filter would look something like:
     * <code>(&(|(attr1=value1)(attr2=value2))(searchAttr=pattern)(objectclassfilter))</code>.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param crestQuery Either a string, coming from something like the CREST endpoint _queryId or a fully
     *                        fledged query filter, coming from a CREST endpoint's _queryFilter
     * @param maxTime The time limit for this search (in seconds). When maxTime &lt; 1, the default time limit will
     * be used.
     * @param maxResults The number of maximum results we should receive for this search. When maxResults &lt; 1 the
     * default sizelimit will be used.
     * @param returnAttrs The attributes that should be returned from the "search hits".
     * @param returnAllAttrs <code>true</code> if all user attribute should be returned.
     * @param filterOp When avPairs is provided, this logical operation will be used between them. Use
     * {@link IdRepo#AND_MOD} or {@link IdRepo#OR_MOD}.
     * @param avPairs Attribute-value pairs based on the search should be performed.
     * @param recursive Deprecated setting, not used.
     * @return The search results based on the provided parameters.
     * @throws IdRepoException Shouldn't be thrown as the returned RepoSearchResults will contain the error code.
     */
    @Override
    public RepoSearchResults search(SSOToken token, IdType type, CrestQuery crestQuery, int maxTime,
                                    int maxResults, Set<String> returnAttrs, boolean returnAllAttrs, int filterOp,
                                    Map<String, Set<String>> avPairs, boolean recursive)
            throws IdRepoException {

        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("search invoked with type: " + type
                    + " crestQuery: " + crestQuery
                    + " avPairs: " + avPairs
                    + " maxTime: " + maxTime
                    + " maxResults: " + maxResults
                    + " returnAttrs: " + returnAttrs
                    + " returnAllAttrs: " + returnAllAttrs
                    + " filterOp: " + filterOp
                    + " recursive: " + recursive);
        }
        Dn baseDN = getBaseDN(type);
        // Recursive is a deprecated setting on IdSearchControl, hence we should use the searchscope defined in the
        // datastore configuration.
        SearchScope scope = defaultScope;

        String searchAttr = getSearchAttribute(type);
        String[] attrs;
        List<Filter> filters = new ArrayList<>();

        if (crestQuery.hasQueryId()) {
            filters.add(Filter.valueOf(searchAttr + "=" + partiallyEscapeAssertionValue(crestQuery.getQueryId())));
        } else if (!crestQuery.getQueryFilter().equals(QueryFilter.alwaysTrue())) {
            filters.add(crestQuery.getQueryFilter().accept(new LdapFromJsonQueryFilterVisitor(getSearchAttribute(type)),
                                                           null));
        }

        filters.add(getObjectClassFilter(type));

        Filter filter = Filter.and(filters);
        Filter tempFilter = constructFilter(filterOp, avPairs);
        if (tempFilter != null) {
            filter = Filter.and(tempFilter, filter);
        }
        if (returnAllAttrs || (returnAttrs != null && returnAttrs.contains("*"))) {
            Set<String> predefinedAttrs = getDefinedAttributes(type);
            predefinedAttrs.add(searchAttr);
            attrs = predefinedAttrs.toArray(new String[predefinedAttrs.size()]);
            returnAllAttrs = true;
        } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
            returnAttrs.add(searchAttr);
            attrs = returnAttrs.toArray(new String[returnAttrs.size()]);
        } else {
            attrs = new String[]{searchAttr};
        }
        SearchRequest searchRequest = LDAPRequests.newSearchRequest(baseDN, scope, filter, attrs);
        searchRequest.setSizeLimit(maxResults < 1 ? defaultSizeLimit : maxResults);
        searchRequest.setTimeLimit(maxTime < 1 ? defaultTimeLimit : maxTime);
        Set<String> names = new HashSet<>();
        Map<String, Map<String, Set<String>>> entries = new HashMap<>();
        int errorCode = RepoSearchResults.SUCCESS;
        try (Connection conn = createConnection()) {
            ConnectionEntryReader reader = conn.search(searchRequest);
            while (reader.hasNext()) {
                Map<String, Set<String>> attributes = new HashMap<>();
                if (reader.isEntry()) {
                    SearchResultEntry entry = reader.readEntry();
                    String name = entry.parseAttribute(searchAttr).asString();
                    names.add(name);
                    if (returnAllAttrs) {
                        for (Attribute attribute : entry.getAllAttributes()) {
                            LDAPUtils.addAttributeToMapAsString(attribute, attributes);
                        }
                        entries.put(name, attributes);
                    } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
                        for (String attr : returnAttrs) {
                            Attribute attribute = entry.getAttribute(attr);
                            if (attribute != null) {
                                LDAPUtils.addAttributeToMapAsString(attribute, attributes);
                            }
                        }
                        entries.put(name, attributes);
                    } else {
                        //there is no attribute to return, don't populate the entries map
                    }
                } else {
                    //ignore search result references
                    reader.readReference();
                }
            }
        } catch (LdapException ere) {
            ResultCode resultCode = ere.getResult().getResultCode();
            if (resultCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                return new RepoSearchResults(new HashSet<String>(0), RepoSearchResults.SUCCESS,
                        Collections.EMPTY_MAP, type);
            } else if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)
                        || resultCode.equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                errorCode = RepoSearchResults.TIME_LIMIT_EXCEEDED;
            } else if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                errorCode = RepoSearchResults.SIZE_LIMIT_EXCEEDED;
            } else {
                DEBUG.error("Unexpected error occurred during search. {}", getConnectionInformation(), ere);
                errorCode = resultCode.intValue();
            }
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
        }
        return new RepoSearchResults(names, errorCode, entries, type);
    }

    /**
     * Deletes the identity from the directory.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @throws IdRepoException If the identity cannot be found, or there is an error while deleting the identity.
     */
    @Override
    public void delete(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("delete invoked");
        }
        Dn dn = getDNSkipExistenceCheck(type, name);
        try (Connection conn = createConnection()) {
            conn.delete(LDAPRequests.newDeleteRequest(dn));
        } catch (LdapException ere) {
            DEBUG.error("Unable to delete entry: " + dn, ere);
            handleErrorResult(ere);
        }
        if (dnCacheEnabled) {
            dnCache.invalidate(generateDNCacheKey(name, type));
        }
    }

    /**
     * Gets membership data for a given group/role/filtered role.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always GROUP, ROLE or FILTEREDROLE.
     * @param name The name of the identity.
     * @param membersType The type of the member identity, this should be always USER.
     * @return The DNs of the members.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity type is not GROUP/ROLE/FILTEREDROLE,</li>
     *  <li>the membersType is not USER,</li>
     *  <li>the identity cannot be found,</li>
     *  <li>there was an error while retrieving the members.</li>
     * </ul>
     */
    @Override
    public Set<String> getMembers(SSOToken token, IdType type, String name, IdType membersType)
            throws IdRepoException {
        DEBUG.debug("{}.{}: Type: {}, Name: {}, Member type: {}",
            DEBUG_CLASS_NAME, "getMembers", type, name, membersType);
        if (type.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIP_TO_USERS_AND_AGENTS_NOT_ALLOWED);
        }
        if (!membersType.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIP_NOT_SUPPORTED, CLASS_NAME, membersType.getName(),
                    type.getName());
        }

        Dn dn = getDNSkipExistenceCheck(type, name);
        if (type.equals(IdType.GROUP)) {
            return getGroupMembers(dn);
        } else if (type.equals(IdType.ROLE)) {
            return getRoleMembers(dn);
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return getFilteredRoleMembers(dn);
        }
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                new Object[]{CLASS_NAME, IdOperation.READ.getName(), type.getName()});
    }

    /**
     * Returns the DNs of the members of this group. If the MemberURL attribute has been configured, then this
     * will also try to retrieve dynamic group members using the memberURL.
     *
     * @param dn The DN of the group to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the members.
     */
    private Set<String> getGroupMembers(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: Group: {}", DEBUG_CLASS_NAME, "getGroupMembers", dn);
        Set<String> results = new HashSet<>();
        String[] attrs;
        if (memberURLAttr != null) {
            attrs = new String[]{uniqueMemberAttr, memberURLAttr};
        } else {
            attrs = new String[]{uniqueMemberAttr};
        }
        try (Connection conn = createConnection()) {
            SearchResultEntry entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn, attrs));
            Attribute attr = entry.getAttribute(uniqueMemberAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            } else if (memberURLAttr != null) {
                attr = entry.getAttribute(memberURLAttr);
                if (attr != null) {
                    for (ByteString byteString : attr) {
                        LdapUrl url = LdapUrl.valueOf(byteString.toString());
                        SearchRequest searchRequest = LDAPRequests.newSearchRequest(
                                url.getName(), url.getScope(), url.getFilter(), DN_ATTR);
                        searchRequest.setTimeLimit(defaultTimeLimit);
                        searchRequest.setSizeLimit(defaultSizeLimit);
                        DEBUG.debug("{}.{}: LDAP Search: TimeLimit: {}, SizeLimit: {}",
                            DEBUG_CLASS_NAME, "getGroupMembers", defaultTimeLimit, defaultSizeLimit);
                        ConnectionEntryReader reader = conn.search(searchRequest);
                        while (reader.hasNext()) {
                            if (reader.isEntry()) {
                                results.add(reader.readEntry().getName().toString());
                            } else {
                                //ignore search result references
                                reader.readReference();
                            }
                        }
                    }
                }
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while retrieving group members for " + dn, ere);
            handleErrorResult(ere);
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getGroupMembers", results);
        }
        return results;
    }

    /**
     * Returns the DNs of the members of this role. To do that this will execute an LDAP search with a filter looking
     * for nsRoleDN=roleDN.
     *
     * @param dn The DN of the role to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the role members.
     */
    private Set<String> getRoleMembers(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: Role: {}", DEBUG_CLASS_NAME, "getRoleMembers", dn);
        Set<String> results = new HashSet<>();
        Dn roleBase = getBaseDN(IdType.ROLE);
        Filter filter = Filter.equality(roleDNAttr, dn);
        SearchRequest searchRequest = LDAPRequests.newSearchRequest(roleBase, roleScope, filter, DN_ATTR);
        searchRequest.setTimeLimit(defaultTimeLimit);
        searchRequest.setSizeLimit(defaultSizeLimit);
        DEBUG.debug("{}.{}: LDAP Search: Base: {}, Scope: {}, Filter: {}, TimeLimit: {}, SizeLimit: {}",
            DEBUG_CLASS_NAME, "getRoleMembers", roleBase, roleScope, filter, defaultTimeLimit, defaultSizeLimit);
        try (Connection conn = createConnection()) {
            ConnectionEntryReader reader = conn.search(searchRequest);
            while (reader.hasNext()) {
                if (reader.isEntry()) {
                    results.add(reader.readEntry().getName().toString());
                } else {
                    //ignore search result references
                    reader.readReference();
                }
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role members for " + dn, ere);
            handleErrorResult(ere);
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getRoleMembers", results);
        }
        return results;
    }

    /**
     * Returns the DNs of the members of this filtered role. To do that this will execute a read on the filtered role
     * entry to get the values of the nsRoleFilter attribute, and then it will perform searches using the retrieved
     * filters.
     *
     * @param dn The DN of the filtered role to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the filtered role members.
     */
    private Set<String> getFilteredRoleMembers(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: Role: {}", DEBUG_CLASS_NAME, "getFilteredRoleMembers", dn);
        Set<String> results = new HashSet<>();
        try (Connection conn = createConnection()) {
            SearchResultEntry entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn,
                    roleFilterAttr));
            Attribute filterAttr = entry.getAttribute(roleFilterAttr);
            if (filterAttr != null) {
                for (ByteString byteString : filterAttr) {
                    Filter filter = Filter.valueOf(byteString.toString());
                    //TODO: would it make sense to OR these filters and run a single search?
                    SearchRequest searchRequest =
                            LDAPRequests.newSearchRequest(rootSuffix, defaultScope, filter.toString(), DN_ATTR);
                    searchRequest.setTimeLimit(defaultTimeLimit);
                    searchRequest.setSizeLimit(defaultSizeLimit);
                    DEBUG.debug("{}.{}: LDAP Search: Base: {}, Scope: {}, Filter: {}, TimeLimit: {}, SizeLimit: {}",
                        DEBUG_CLASS_NAME, "getFilteredRoleMembers", rootSuffix, defaultScope, filter,
                            defaultTimeLimit, defaultSizeLimit);
                    ConnectionEntryReader reader = conn.search(searchRequest);
                    while (reader.hasNext()) {
                        if (reader.isEntry()) {
                            results.add(reader.readEntry().getName().toString());
                        } else {
                            //ignore search result references
                            reader.readReference();
                        }
                    }
                }
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role members for " + dn, ere);
            handleErrorResult(ere);
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getFilteredRoleMembers", results);
        }
        return results;
    }

    /**
     * Returns the membership information of a user for the given membership type.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param membershipType The type of the membership identity, this should be always GROUP/ROLE/FILTEREDROLE.
     * @return The DNs of the groups/roles/filtered roles this user is member of.
     * @throws IdRepoException if the identity type is not USER, or if there was an error while retrieving the
     * membership data.
     */
    @Override
    public Set<String> getMemberships(SSOToken token, IdType type, String name, IdType membershipType)
            throws IdRepoException {
        DEBUG.debug("{}.{}: User Type: {}, Name: {}, Membership type: {}",
            DEBUG_CLASS_NAME, "getMemberships", type, name, membershipType);
        if (!type.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED, CLASS_NAME);
        }
        Dn dn = getDNSkipExistenceCheck(USER, name);
        if (membershipType.equals(IdType.GROUP)) {
            return getGroupMemberships(dn);
        } else if (membershipType.equals(IdType.ROLE)) {
            return getRoleMemberships(dn);
        } else if (membershipType.equals(IdType.FILTEREDROLE)) {
            return getFilteredRoleMemberships(dn);
        }
        throw newIdRepoException(IdRepoErrorCode.MEMBERSHIP_NOT_SUPPORTED, CLASS_NAME, type.getName(),
                membershipType.getName());
    }

    /**
     * Returns the group membership informations for this given user. In case the memberOf attribute is configured,
     * this will try to query the user entry and return the group DNs found in the memberOf attribute. Otherwise a
     * search request will be issued using the uniqueMember attribute looking for matches with the user DN.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the groups that the provided user is member of.
     * @throws IdRepoException If there was an error while retrieving the group membership information.
     */
    private Set<String> getGroupMemberships(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: User: {}", DEBUG_CLASS_NAME, "getGroupMemberships", dn);
        Set<String> results = new HashSet<>();
        if (memberOfAttr == null || adRecursiveGroupMembershipsEnabled) {
            Filter uniqueMemberFilter;
            if (adRecursiveGroupMembershipsEnabled) {
                uniqueMemberFilter = Filter.extensible(AD_LDAP_MATCHING_RULE_IN_CHAIN_OID, uniqueMemberAttr,
                    dn, false);
            } else {
                uniqueMemberFilter = Filter.equality(uniqueMemberAttr, dn);
            }
            Filter filter = Filter.and(groupSearchFilter, uniqueMemberFilter);
            SearchRequest searchRequest =
                    LDAPRequests.newSearchRequest(getBaseDN(IdType.GROUP), defaultScope, filter, DN_ATTR);
            searchRequest.setTimeLimit(defaultTimeLimit);
            searchRequest.setSizeLimit(defaultSizeLimit);
            DEBUG.debug("{}.{}: LDAP Search: Base: {}, Scope: {}, Filter: {}, TimeLimit: {}, SizeLimit: {}",
                DEBUG_CLASS_NAME, "getGroupMemberships", getBaseDN(IdType.GROUP), defaultScope, filter,
                defaultTimeLimit, defaultSizeLimit);
            try (Connection conn = createConnection()) {
                ConnectionEntryReader reader = conn.search(searchRequest);
                while (reader.hasNext()) {
                    if (reader.isEntry()) {
                        results.add(reader.readEntry().getName().toString());
                    } else {
                        //ignore search result references
                        reader.readReference();
                    }
                }
            } catch (LdapException ere) {
                DEBUG.error("An error occurred while trying to retrieve group memberships for " + dn
                        + " using " + uniqueMemberAttr, ere);
                handleErrorResult(ere);
            } catch (SearchResultReferenceIOException srrioe) {
                //should never ever happen...
                DEBUG.error("Got reference instead of entry", srrioe);
                throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
            }
        } else {
            DEBUG.debug("{}.{}: LDAP Search: Base: {}, Attrs: {}",
                DEBUG_CLASS_NAME, "getGroupMemberships", dn, memberOfAttr);
            try (Connection conn = createConnection()) {
                SearchResultEntry entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn,
                        memberOfAttr));
                Attribute attr = entry.getAttribute(memberOfAttr);
                if (attr != null) {
                    results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
                }
            } catch (LdapException ere) {
                DEBUG.error("An error occurred while trying to retrieve group memberships for " + dn
                        + " using " + memberOfAttr + " attribute", ere);
                handleErrorResult(ere);
            }
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getGroupMemberships", results);
        }
        return results;
    }

    /**
     * Return the role membership informations for this given user. This will execute a read on the user entry to
     * retrieve the nsRoleDN attribute. The values of the attribute will be returned.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the roles this user is member of.
     * @throws IdRepoException If there was an error while retrieving the role membership information.
     */
    private Set<String> getRoleMemberships(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: User: {}", DEBUG_CLASS_NAME, "getRoleMemberships", dn);
        Set<String> results = new HashSet<>();
        try (Connection conn = createConnection()) {
            DEBUG.debug("{}.{}: LDAP Search: Base: {}, Attrs: {}",
                DEBUG_CLASS_NAME, "getRoleMemberships", dn, roleDNAttr);
            SearchResultEntry entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn, roleDNAttr));
            Attribute attr = entry.getAttribute(roleDNAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to retrieve role memberships for " + dn
                    + " using " + roleDNAttr + " attribute", ere);
            handleErrorResult(ere);
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getRoleMemberships", results);
        }
        return results;
    }

    /**
     * Returns the filtered and non-filtered role memberships for this given user. This will execute a read on the user
     * entry to retrieve the nsRole attribute. The values of the attribute will be returned along with the non-filtered
     * role memberships.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the filtered roles this user is member of.
     * @throws IdRepoException If there was an error while retrieving the filtered or non-filtered role membership
     * information.
     */
    private Set<String> getFilteredRoleMemberships(Dn dn) throws IdRepoException {
        DEBUG.debug("{}.{}: User: {}", DEBUG_CLASS_NAME, "getFilteredRoleMemberships", dn);
        Set<String> results = new CaseInsensitiveHashSet<>();
        try (Connection conn = createConnection()) {
            DEBUG.debug("{}.{}: LDAP Search: Base: {}, Attrs: {}",
                    DEBUG_CLASS_NAME, "getFilteredRoleMemberships", dn, roleAttr);
            SearchResultEntry entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn, roleAttr));
            Attribute attr = entry.getAttribute(roleAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role memberships for " + dn
                    + " using " + roleAttr + " attribute", ere);
            handleErrorResult(ere);
        }
        results.addAll(getRoleMemberships(dn));
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("{}.{}: Results: {}", DEBUG_CLASS_NAME, "getFilteredRoleMemberships", results);
        }

        return results;
    }

    /**
     * Adds or removes members to the provided group/role.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always GROUP or ROLE.
     * @param name The name of the identity.
     * @param members The set of members that needs to be added/removed.
     * @param membersType The type of the member, this should be always USER.
     * @param operation The operation that needs to be performed with the provided members. Use {@link
     * IdRepo#ADDMEMBER} and {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>there are no members provided,</li>
     *  <li>the provided type/membersType are invalid,</li>
     *  <li>the identity to be modified cannot be found,</li>
     *  <li>one of the members cannot be found,</li>
     *  <li>there was an error while trying to modify the membership data.</li>
     * </ul>
     */
    @Override
    public void modifyMemberShip(SSOToken token, IdType type, String name, Set<String> members, IdType membersType,
            int operation) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("modifymembership invoked");
        }
        if (members == null || members.isEmpty()) {
            throw newIdRepoException(IdRepoErrorCode.ILLEGAL_ARGUMENTS);
        }
        if (type.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIP_TO_USERS_AND_AGENTS_NOT_ALLOWED);
        }
        if (!membersType.equals(USER)) {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED, CLASS_NAME);
        }
        Dn dn = getDNSkipExistenceCheck(type, name);
        Set<Dn> memberDNs = new HashSet<>(members.size());
        for (String member : members) {
            memberDNs.add(getDN(membersType, member));
        }
        if (type.equals(IdType.GROUP)) {
            modifyGroupMembership(dn, memberDNs, operation);
        } else if (type.equals(IdType.ROLE)) {
            modifyRoleMembership(dn, memberDNs, operation);
        } else {
            throw newIdRepoException(IdRepoErrorCode.MEMBERSHIP_CANNOT_BE_MODIFIED, CLASS_NAME, type.getName());
        }
    }

    /**
     * Modifies group membership data in the directory. In case the memberOf attribute is configured, this will also
     * iterate through all the user entries and modify those as well. Otherwise this will only modify the uniquemember
     * attribute on the group entry based on the operation.
     *
     * @param groupDN The DN of the group.
     * @param memberDNs The DNs of the group members.
     * @param operation Whether the members needs to be added or removed from the group. Use {@link IdRepo#ADDMEMBER}
     * or {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException If there was an error while modifying the membership data.
     */
    private void modifyGroupMembership(Dn groupDN, Set<Dn> memberDNs, int operation) throws IdRepoException {
        ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(groupDN);
        Attribute attr = new LinkedAttribute(uniqueMemberAttr, memberDNs);
        ModificationType modType;
        if (ADDMEMBER == operation) {
            modType = ModificationType.ADD;
        } else {
            modType = ModificationType.DELETE;
        }
        modifyRequest.addModification(new Modification(modType, attr));
        try (Connection conn = createConnection()) {
            conn.modify(modifyRequest);
            if (memberOfAttr != null) {
                for (Dn member : memberDNs) {
                    ModifyRequest userMod = LDAPRequests.newModifyRequest(member);
                    userMod.addModification(modType, memberOfAttr, groupDN);
                    conn.modify(userMod);
                }
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to modify group membership. Name: " + groupDN
                    + " memberDNs: " + memberDNs + " Operation: " + modType, ere);
            handleErrorResult(ere);
        }

    }

    /**
     * Modifies role membership data in the directory. This will add/remove the corresponding nsRoleDN attribute from
     * the user entry.
     *
     * @param roleDN The DN of the role.
     * @param memberDNs The DNs of the role members.
     * @param operation Whether the members needs to be added or removed from the group. Use {@link IdRepo#ADDMEMBER}
     * or {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException If there was an error while modifying the membership data.
     */
    private void modifyRoleMembership(Dn roleDN, Set<Dn> memberDNs, int operation) throws IdRepoException {
        Attribute attr = new LinkedAttribute(roleDNAttr, roleDN);
        Modification mod;
        if (ADDMEMBER == operation) {
            mod = new Modification(ModificationType.ADD, attr);
        } else {
            mod = new Modification(ModificationType.DELETE, attr);
        }
        try (Connection conn = createConnection()) {
            for (Dn memberDN : memberDNs) {
                conn.modify(LDAPRequests.newModifyRequest(memberDN).addModification(mod));
            }
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to modify role membership. Name: " + roleDN
                    + " memberDNs: " + memberDNs, ere);
            handleErrorResult(ere);
        }
    }

    /**
     * Assigns a service to the provided identity.
     * In case of a USER if the attribute map contains objectclasses, then
     * the existing set of objectclasses will be retrieved, and added to those. These settings will override the
     * existing values if any present.
     * In case of a REALM the service attributes will be persisted by the {@link IdRepoListener} implementation.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service that needs to be assigned to the identity.
     * @param stype The schema type of the service that needs to be assigned.
     * @param attrMap The service configuration that needs to be saved for the identity.
     * @throws IdRepoException If there was an error while retrieving the user objectclasses, or when the settings were
     * being saved to the identity.
     */
    @Override
    public void assignService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype,
            Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("assignService invoked");
        }
        if (type.equals(USER)) {
            Set<String> ocs = attrMap.get(OBJECT_CLASS_ATTR);
            if (stype.equals(SchemaType.USER)) {
                if (ocs != null) {
                    Map<String, Set<String>> attrs = getAttributes(token, type, name, asSet(OBJECT_CLASS_ATTR));
                    ocs = new CaseInsensitiveHashSet<>(ocs);
                    ocs.addAll(attrs.get(OBJECT_CLASS_ATTR));
                    attrMap.put(OBJECT_CLASS_ATTR, ocs);
                }
                setAttributes(token, type, name, attrMap, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            if (serviceName != null && !serviceName.isEmpty() && attrMap != null) {
                serviceMap.put(serviceName, new HashMap<>(attrMap));
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, new Object[]{CLASS_NAME});
        }
    }

    /**
     * Returns the currently assigned to the given identity.
     * In case of a USER this will retrieve the objectclasses defined for this user, and based on the provided
     * mapOfServicesAndOCs if all of the objectclasses mapped to a service is present, only then will the service be
     * added to the resulting list.
     * In case of a REALM the locally stored serviceMap's keySet will be returned, since that contains all the different
     * service names defined within this realm.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param mapOfServicesAndOCs A mapping between the names of services and the corresponding objectclasses.
     * @return The list of services that are currently assigned to the identity.
     * @throws IdRepoException If the identity type was invalid, or if there was an error while retrieving the
     * objectclasses.
     */
    @Override
    public Set<String> getAssignedServices(SSOToken token, IdType type, String name,
            Map<String, Set<String>> mapOfServicesAndOCs) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getAssignedServices invoked for the following services and objectClasses: {}",
                    mapOfServicesAndOCs);
        }
        Set<String> results = new HashSet<>();
        if (type.equals(USER)) {
            Set<String> attrs = asSet("objectclass");
            Set<String> objectClasses = getAttributes(token, type, name, attrs).get(OBJECT_CLASS_ATTR);
            if (CollectionUtils.isNotEmpty(objectClasses)) {
                Set<String> ocValues = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                ocValues.addAll(objectClasses);
                for (Map.Entry<String, Set<String>> entry : mapOfServicesAndOCs.entrySet()) {
                    String serviceName = entry.getKey();
                    Set<String> serviceOCs = entry.getValue();
                    if (ocValues.containsAll(serviceOCs)) {
                        results.add(serviceName);
                    }
                }
            }
        } else if (type.equals(IdType.REALM)) {
            results.addAll(serviceMap.keySet());
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, new Object[]{CLASS_NAME});
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("Assigned services returned: " + results);
        }
        return results;
    }

    /**
     * Returns the service attributes in string format for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    @Override
    public Map<String, Set<String>> getServiceAttributes(SSOToken token, IdType type, String name, String serviceName,
            Set<String> attrNames) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getServiceAttributes invoked");
        }
        return getServiceAttributes(type, name, serviceName, attrNames, new StringAttributeExtractor(),
                new StringToStringConverter());
    }

    /**
     * Returns the service attributes in binary format for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    @Override
    public Map<String, byte[][]> getBinaryServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set<String> attrNames) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("getBinaryServiceAttributes invoked");
        }
        return getServiceAttributes(type, name, serviceName, attrNames, new BinaryAttributeExtractor(),
                new StringToBinaryConverter());
    }

    /**
     * Returns the service attributes in binary or string format for the given identity.
     * In case of a USER this will retrieve first the service attributes from the user entry, and later it will also
     * query the service attributes of the current realm. When a user-specific setting is missing the realm-specific one
     * will be returned instead.
     * In case of a REALM it will return a defensive copy of the service attributes stored locally.
     *
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @param extractor The attribute extractor to use.
     * @param converter The attribute filter to use.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    private <T> Map<String, T> getServiceAttributes(IdType type, String name, String serviceName,
            Set<String> attrNames, Function<Attribute, T, IdRepoException> extractor,
            Function<Map<String, Set<String>>, Map<String, T>, IdRepoException> converter) throws IdRepoException {
        if (type.equals(USER)) {
            Map<String, T> attrsFromUser = getAttributes(type, name, attrNames, extractor);
            if (StringUtils.isEmpty(serviceName)) {
                return attrsFromUser;
            }
            Map<String, Set<String>> attrsFromRealm = serviceMap.get(serviceName);
            Map<String, Set<String>> filteredAttrsFromRealm = new HashMap<>();
            if (CollectionUtils.isEmpty(attrsFromRealm)) {
                return attrsFromUser;
            } else {
                attrNames = new CaseInsensitiveHashSet<>(attrNames);
                for (Map.Entry<String, Set<String>> entry : attrsFromRealm.entrySet()) {
                    String attrName = entry.getKey();
                    if (attrNames.contains(attrName)) {
                        filteredAttrsFromRealm.put(attrName, entry.getValue());
                    }
                }
            }

            Map<String, T> filteredAttrsFromRealm2 = converter.apply(filteredAttrsFromRealm);
            Set<String> attrNameSet = attrsFromUser.keySet();
            for (Map.Entry<String, T> entry : filteredAttrsFromRealm2.entrySet()) {
                String attrName = entry.getKey();
                if (!attrNameSet.contains(attrName)) {
                    attrsFromUser.put(attrName, entry.getValue());
                }
            }
            return attrsFromUser;
        } else if (type.equals(IdType.REALM)) {
            Map<String, T> attrs = converter.apply(serviceMap.get(serviceName));
            if (attrs == null || attrs.isEmpty()) {
                return new HashMap<>();
            } else if (attrNames == null || attrNames.isEmpty()) {
                return new HashMap<>(attrs);
            } else {
                Map<String, T> results = new HashMap<>();
                Set<String> attributeNames = new CaseInsensitiveHashSet<>(attrs.keySet());
                for (String attrName : attrNames) {
                    if (attributeNames.contains(attrName)) {
                        results.put(attrName, attrs.get(attrName));
                    }
                }
                return results;
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, new Object[]{CLASS_NAME});
        }
    }

    /**
     * Modifies the service attributes based on the incoming attributeMap.
     * In case of a USER the attributes will be saved in case the schema type is not DYNAMIC.
     * In case of a REALM this will only modify the locally stored Map structure by making sure that non-modified
     * attributes are kept.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service that needs to be modified.
     * @param sType The type of the service schema.
     * @param attrMap The attributes that needs to be set for the service.
     * @throws IdRepoException If the type was invalid, or if there was an error while setting the service attributes.
     */
    @Override
    public void modifyService(SSOToken token, IdType type, String name, String serviceName,
            SchemaType sType, Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("modifyService invoked");
        }
        if (type.equals(USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                throw newIdRepoException(IdRepoErrorCode.CANNOT_MODIFY_SERVICE, CLASS_NAME, sType.toString(),
                        type.getName());
            } else {
                setAttributes(token, type, name, attrMap, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            Map<String, Set<String>> previousAttrs = serviceMap.get(serviceName);
            if (previousAttrs == null || previousAttrs.isEmpty()) {
                serviceMap.put(serviceName, new HashMap<>(attrMap));
            } else {
                Set<String> previousAttrNames = new CaseInsensitiveHashSet<>(previousAttrs.keySet());
                for (Map.Entry<String, Set<String>> entry : attrMap.entrySet()) {
                    String attrName = entry.getKey();
                    Set<String> values = entry.getValue();
                    if (previousAttrNames.contains(attrName)) {
                        Set<String> current = previousAttrs.get(attrName);
                        current.clear();
                        current.addAll(values);
                    } else {
                        previousAttrs.put(attrName, values);
                    }
                }
                serviceMap.put(serviceName, previousAttrs);
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, new Object[]{CLASS_NAME});
        }
    }

    /**
     * Unassigns a service from the provided identity.
     * In case of a USER this will traverse through all the existing user attributes and will remove those that are
     * currently present in the entry. This will also remove the objectclass corresponding to the service.
     * In case of a REALM this will remove the service from the locally cached serviceMap, and will notify the
     * registered {@link IdRepoListener}.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service to remove from the identity.
     * @param attrMap Holds the objectclasses relevant for this service removal.
     * @throws IdRepoException If the identity type was invalid or if there was an error while removing the service.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void unassignService(SSOToken token, IdType type, String name, String serviceName,
            Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("unassignService invoked");
        }
        if (type.equals(USER)) {
            Set<String> removeOCs = attrMap.get(OBJECT_CLASS_ATTR);
            if (removeOCs != null) {
                Schema dirSchema = getSchema();
                Map<String, Set<String>> attrs = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
                for (String oc : removeOCs) {
                    try {
                        ObjectClass oc2 = dirSchema.getObjectClass(oc);
                        for (AttributeType optional : oc2.getDeclaredOptionalAttributes()) {
                            attrs.put(optional.getNameOrOid(), Collections.emptySet());
                        }
                        for (AttributeType required : oc2.getDeclaredRequiredAttributes()) {
                            attrs.put(required.getNameOrOid(), Collections.emptySet());
                        }
                    } catch (UnknownSchemaElementException usee) {
                        DEBUG.error("Unable to unassign " + serviceName + " service from identity: " + name, usee);
                        throw newIdRepoException(IdRepoErrorCode.UNABLE_GET_SERVICE_SCHEMA, serviceName);
                    }
                }
                Set<String> ocKeys = attrs.keySet();
                Set<String>requestedAttrs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                requestedAttrs.addAll(ocKeys);
                //if the service objectclass is auxiliary (which it should be), then the objectclass attribute may not
                //be present if top is not defined as superior class.
                requestedAttrs.add(OBJECT_CLASS_ATTR);
                Map<String, Set<String>> attributes = getAttributes(token, type, name, requestedAttrs);
                Set<String> ocValues = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                ocValues.addAll(attributes.get(OBJECT_CLASS_ATTR));
                ocValues.removeAll(removeOCs);
                attrs.put(OBJECT_CLASS_ATTR, ocValues);
                //we need to only change existing attributes, removal of a non-existing attribute results in failure.
                //implementing retainAll here for CaseInsensitiveHashMap's keySet
                for (String string : attrs.keySet()) {
                    if (!attributes.containsKey(string)) {
                        attrs.remove(string);
                    }
                }
                setAttributes(token, type, name, attrs, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            if (serviceName != null && !serviceName.isEmpty()) {
                serviceMap.remove(serviceName);
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, new Object[]{CLASS_NAME});
        }
    }

    /**
     * Registers an IdRepoListener, which will be notified of realm level service changes and persistent search results.
     * If persistent search is not yet established with the current settings, this will create a new persistent search
     * against the configured directory.
     *
     * @param token Not used.
     * @param idRepoListener The IdRepoListener that will be used to notify about service changes and persistent search
     * results.
     * @return Always returns <code>0</code>.
     */
    @Override
    public int addListener(SSOToken token, IdRepoListener idRepoListener) {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("addListener invoked");
        }
        //basically we should have only one idRepoListener per IdRepo
        if (this.idRepoListener != null) {
            throw new IllegalStateException("There is an idRepoListener already registered within this IdRepo");
        }

        this.idRepoListener = idRepoListener;
        String psearchBaseDN = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN);
        if (StringUtils.isEmpty(psearchBaseDN)) {
            if (DEBUG.isWarnEnabled()) {
                DEBUG.warn("Persistent search base DN is missing, persistent search is disabled.");
            }
            return 0;
        }

        String pSearchId = getPSearchId();
        synchronized (PERSISTENT_SEARCH_MAP) {
            DJLDAPv3PersistentSearch pSearch = PERSISTENT_SEARCH_MAP.get(pSearchId);
            if (pSearch == null) {
                String username = CollectionHelper.getMapAttr(configMap, LDAP_SERVER_USER_NAME);
                char[] password = CollectionHelper.getMapAttr(configMap, LDAP_SERVER_PASSWORD, "").toCharArray();
                int minPoolSize = CollectionHelper.getIntMapAttr(configMap, LDAP_CONNECTION_POOL_MIN_SIZE, 1, DEBUG);
                pSearch = new DJLDAPv3PersistentSearch(configMap, createConnectionFactory(username, password,
                    Math.min(minPoolSize, 1), 2));
                if (dnCacheEnabled) {
                    pSearch.addMovedOrRenamedListener(this);
                }
                pSearch.addListener(idRepoListener, getSupportedTypes());
                try {
                    pSearch.startQuery();
                } catch (DataLayerException e) {
                    DEBUG.warn("Persistent search failure to attach connection.", e);
                }
                PERSISTENT_SEARCH_MAP.put(pSearchId, pSearch);
            } else {
                pSearch.addListener(idRepoListener, getSupportedTypes());
                if (dnCacheEnabled) {
                    pSearch.addMovedOrRenamedListener(this);
                }
            }
        }
        return 0;
    }

    /**
     * This method will be called by the end of the IdRepo's lifetime, and makes sure that persistent search is properly
     * terminated for this IdRepo.
     */
    @Override
    public void removeListener() {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("removelistener invoked");
        }
        String psearchBaseDN = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN);
        if (StringUtils.isEmpty(psearchBaseDN)) {
            if (DEBUG.isDebugEnabled()) {
                DEBUG.debug("Persistent search is disabled, no need to unregister.");
            }
        } else {
            String pSearchId = getPSearchId();
            DJLDAPv3PersistentSearch pSearchToStop = null;
            synchronized (PERSISTENT_SEARCH_MAP) {
                DJLDAPv3PersistentSearch pSearch = PERSISTENT_SEARCH_MAP.get(pSearchId);
                if (pSearch == null) {
                    DEBUG.debug("PSearch is already removed, unable to unregister");
                } else {
                    pSearch.removeMovedOrRenamedListener(this);
                    pSearch.removeListener(idRepoListener);
                    if (!pSearch.hasListeners()) {
                        pSearchToStop = pSearch;
                        PERSISTENT_SEARCH_MAP.remove(pSearchId, pSearch);
                    }
                }
            }
            if (pSearchToStop != null) {
                pSearchToStop.stopSearch();
            }
        }
        if (dnCacheEnabled) {
            DEBUG.debug("There are {} entries in DN cache. Clearing the cache.", dnCache.size());
            dnCache.invalidateAll();
        }
    }

    /**
     * This method is being invoked during OpenAM shutdown and also when the configuration changes and OpenAM needs to
     * reload the Data Store configuration. This mechanism will make sure that the connection pools are closed, and the
     * persistent search is also terminated (if there is no other data store implementation using the same psearch
     * connection.
     */
    @Override
    public void shutdown() {
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("shutdown invoked");
        }
        super.shutdown();
        removeListener();
        IOUtils.closeIfNotNull(connectionFactory);
        IOUtils.closeIfNotNull(bindConnectionFactory);
        idRepoListener = null;
    }

    /**
     * Called if an identity has been renamed or moved within the identity store.
     * @param previousDN The DN of the identity before the move or rename
     * @param identitySearchAttrValue The search attribute value of the entry
     * @param identityNamingAttrValue The naming/authn attribute value of the entry
     */
    @Override
    public void identityMovedOrRenamed(Dn previousDN, String identitySearchAttrValue, String identityNamingAttrValue) {

        if (dnCacheEnabled) {
            String name = LDAPUtils.getName(previousDN);
            for (IdType idType : getSupportedTypes()) {
                String previousId =  generateDNCacheKey(name, idType);
                if (dnCache.getIfPresent(previousId) != null) {
                    dnCache.invalidate(previousId);
                    DEBUG.debug("Removed {} from DN Cache", previousId);
                }
                if (idType.equals(USER)) {
                    if (StringUtils.isNotEmpty(identitySearchAttrValue)) {
                        dnCache.invalidate(generateDNCacheKey(identitySearchAttrValue, idType));
                        DEBUG.debug("Removed search attr value {} from DN Cache", identitySearchAttrValue);
                    }
                    if (StringUtils.isNotEmpty(identityNamingAttrValue)) {
                        dnCache.invalidate(generateDNCacheKey(identityNamingAttrValue, idType));
                        DEBUG.debug("Removed naming attr value {} from DN Cache", identityNamingAttrValue);
                    }
                }
            }
        }
    }

    /**
     * This method constructs a persistent search "key", which will be used to
     * figure out whether there is an existing persistent search for the same
     * ldap server, base DN, filter, scope combination. By doing this we can
     * "reuse" the results of other datastore implementations without the need
     * of two or more persistent search connections with the same parameters.
     *
     * @return a unique ID based on the LDAP URLs, secure protocol version, psearch base DN, filter and
     * scope settings.
     */
    private String getPSearchId() {
        String psearchBase = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN);
        String pfilter = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_FILTER);
        String scope = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_SCOPE);
        //creating a natural order of the ldap servers, so the "key" should be always the same regardless of the server
        //order in the configuration.
        LDAPURL[] servers = ldapServers.toArray(new LDAPURL[ldapServers.size()]);
        Arrays.sort(servers);
        return Arrays.toString(servers) + psearchBase + pfilter + scope + userSearchAttr;
    }

    private void mapUserStatus(IdType type, Map<String, Set<String>> attributes) {
        if (type.equals(USER)) {
            String userStatus = CollectionHelper.getMapAttr(attributes, DEFAULT_USER_STATUS_ATTR);
            Set<String> value = new HashSet<>(1);
            if (userStatus == null || !STATUS_INACTIVE.equalsIgnoreCase(userStatus)) {
                value.add(activeValue);
            } else {
                value.add(inactiveValue);
            }
            attributes.remove(DEFAULT_USER_STATUS_ATTR);
            attributes.put(userStatusAttr, value);
        }
    }

    private void mapCreationAttributes(IdType type, String name, Map<String, Set<String>> attributes) {
        if (type.equals(USER)) {
            for (Map.Entry<String, String> mapping : creationAttributeMapping.entrySet()) {
                String from = mapping.getKey();
                if (!attributes.containsKey(from)) {
                    String to = mapping.getValue();
                    //if the attrname is same as the attrvalue, use the username as the value of the attribute.
                    if (from.equalsIgnoreCase(to)) {
                        attributes.put(from, asSet(name));
                    } else {
                        Set<String> value = attributes.get(to);
                        if (value != null) {
                            attributes.put(from, value);
                        }
                    }
                }
            }
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("After adding creation attributes: attrMap =  "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attributes, null));
        }
    }

    private <V> Map<String, V> removeUndefinedAttributes(IdType type, Map<String, V> attributes) {
        Set<String> predefinedAttrs = getDefinedAttributes(type);

        Map<String, V> filteredMap = new CaseInsensitiveHashMap<>(attributes);
        for (String key : attributes.keySet()) {
            // Always allow the DEFAULT_USER_STATUS_ATTR to cover any mapped isActive logic in envs like AD.
            if (!predefinedAttrs.contains(key) && !DEFAULT_USER_STATUS_ATTR.equalsIgnoreCase(key)) {
                filteredMap.remove(key);
            }
        }
        return filteredMap;
    }

    private Set<String> removeUndefinedAttributes(IdType type, Set<String> attributes) {
        Set<String> predefinedAttrs = getDefinedAttributes(type);
        Set<String> filteredSet = Collections.emptySet();
        if (attributes != null) {
            filteredSet = new CaseInsensitiveHashSet<>(attributes);
        }
        filteredSet.retainAll(predefinedAttrs);
        return filteredSet;
    }

    private Set<String> getDefinedAttributes(IdType type) {
        if (type.equals(USER)) {
            return userAttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            return groupAttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
            return roleAttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleAttributesAllowed;
        }
        return Collections.emptySet();
    }

    private String getNamingAttribute(IdType type) {
        if (type.equals(USER)) {
            return userNamingAttr;
        } else if (type.equals(IdType.GROUP)) {
            return groupNamingAttr;
        } else if (type.equals(IdType.ROLE)) {
            return roleNamingAttr;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleNamingAttr;
        } else {
            return userNamingAttr;
        }
    }

    private String getSearchAttribute(IdType type) {
        if (type.equals(USER)) {
            return userSearchAttr;
        } else {
            return getNamingAttribute(type);
        }
    }

    private Set<String> getNonNullSettingValues(String setting) {
        return configMap.getOrDefault(setting, Collections.emptySet());
    }

    /**
     * Get the DN of en entry based on its type and name.
     * <p>
     * Main difference with {{@link #getDN(IdType, String)}} is that, for performance reason, the DN might not be
     * checked for existence before being returned. As such, this method must be used only if it's safe to deal with a
     * potentially non existing DN. e.g: When the returned DN is used in another LDAP query which will figure-out
     * whether the entry is missing.
     */
    private Dn getDNSkipExistenceCheck(IdType type, String name) throws IdRepoException {
        return getDN(type, name, false, null, true, false);
    }

    private Dn getDN(IdType type, String name) throws IdRepoException {
        return getDN(type, name, false, null);
    }

    private Dn generateDN(IdType type, String name) throws IdRepoException {
        return getDN(type, name, true, null);
    }

    private Dn findDNForAuth(IdType type, String name) throws IdRepoException {
        return getDN(type, name, false, userNamingAttr, false, true);
    }

    private Dn getDN(IdType type, String name, boolean shouldGenerate, String searchAttr) throws IdRepoException {
        return getDN(type, name, shouldGenerate, searchAttr, true, false);
    }

    private Dn getDN(IdType type, String name, boolean shouldGenerate, String searchAttr, boolean useCache,
            boolean skipExistCheck) throws IdRepoException {
        Dn cachedDn  = null;
        if (dnCacheEnabled) {
            if (!useCache) {
                dnCache.invalidate(generateDNCacheKey(name, type));
                DEBUG.debug("getDN, useCache is false. Removed {} from DN Cache", name);
            } else {
                cachedDn = dnCache.getIfPresent(generateDNCacheKey(name, type));
            }
        }
        if (cachedDn != null) {
            return cachedDn;
        }
        Dn dn = null;
        Dn searchBase = getBaseDN(type);

        if (shouldGenerate) {
            return searchBase.child(getSearchAttribute(type), name);
        }

        if (searchAttr == null) {
            searchAttr = getSearchAttribute(type);
        }
        final SearchRequest searchRequest;
        if (SearchScope.SINGLE_LEVEL.equals(defaultScope) && searchAttr.equals(getSearchAttribute(type))) {
            // Given the search is limited to one level under the base-dn, we can directly build the DN of the entry.
            final Dn entryDn = searchBase.child(searchAttr, name);
            if (skipExistCheck) {
                // The DN will probably be used in another LDAP query.
                // Let this other query figure-out whether the entry is missing.
                return entryDn;
            }
            // Perform the search request to make sure the entry exists.
            searchRequest =
                    LDAPRequests.newSingleEntrySearchRequest(searchBase.child(getSearchAttribute(type), name), DN_ATTR);
        } else {
            final Filter filter = Filter.and(Filter.equality(searchAttr, name), getObjectClassFilter(type));
            searchRequest = LDAPRequests.newSearchRequest(searchBase, defaultScope, filter, DN_ATTR);
        }
        try (Connection conn = createConnection()) {
            ConnectionEntryReader reader = conn.search(searchRequest);
            SearchResultEntry entry = null;
            while (reader.hasNext()) {
                if (reader.isEntry()) {
                    if (entry != null) {
                        throw newIdRepoException(ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED,
                                IdRepoErrorCode.LDAP_EXCEPTION_OCCURRED, CLASS_NAME,
                                ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED.intValue());
                    }
                    entry = reader.readEntry();
                } else {
                    //ignore references
                    reader.readReference();
                }
            }
            if (entry == null) {
                DEBUG.debug("Unable to find entry with name: " + name + " under searchbase: " + searchBase
                        + " with scope: " + defaultScope);

                throw new IdentityNotFoundException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.TYPE_NOT_FOUND,
                        ResultCode.CLIENT_SIDE_NO_RESULTS_RETURNED,
                        new Object[]{name, type.getName()});
            }
            dn = entry.getName();
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while querying entry DN", ere);
            handleErrorResult(ere);
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException(IdRepoErrorCode.SEARCH_FAILED, CLASS_NAME);
        }

        if (dnCacheEnabled) {
            dnCache.put(generateDNCacheKey(name, type), dn);
        }
        return dn;
    }

    private Filter getObjectClassFilter(IdType type) {
        if (type.equals(USER)) {
            return userSearchFilter;
        } else if (type.equals(IdType.GROUP)) {
            return groupSearchFilter;
        } else if (type.equals(IdType.ROLE)) {
            return roleSearchFilter;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleSearchFilter;
        } else {
            return userSearchFilter;
        }
    }

    private Set<String> getObjectClasses(IdType type) {
        if (type.equals(USER)) {
            return userObjectClasses;
        } else if (type.equals(IdType.GROUP)) {
            return groupObjectClasses;
        } else if (type.equals(IdType.ROLE)) {
            return roleObjectClasses;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleObjectClasses;
        } else {
            //should never happen
            return Collections.emptySet();
        }
    }

    private Dn getBaseDN(IdType type) {
        Dn dn = Dn.valueOf(rootSuffix);
        if (type.equals(USER) && peopleContainerName != null && !peopleContainerName.isEmpty()
                && peopleContainerValue != null && !peopleContainerValue.isEmpty()) {
            dn = dn.child(peopleContainerName, peopleContainerValue);
        } else if (type.equals(IdType.GROUP) && groupContainerName != null && !groupContainerName.isEmpty()
                && groupContainerValue != null && !groupContainerValue.isEmpty()) {
            dn = dn.child(groupContainerName, groupContainerValue);
        }

        return dn;
    }

    protected Filter constructFilter(int operation, Map<String, Set<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        Set<Filter> filters = new LinkedHashSet<>(attributes.size());
        for (Map.Entry<String, Set<String>> entry : attributes.entrySet()) {
            for (String value : entry.getValue()) {
                filters.add(Filter.valueOf(entry.getKey() + "=" + partiallyEscapeAssertionValue(value)));
            }
        }
        Filter filter;
        switch (operation) {
            case OR_MOD:
                filter = Filter.or(filters);
                break;
            case AND_MOD:
                filter = Filter.and(filters);
                break;
            default:
                //falling back to AND
                filter = Filter.and(filters);
        }
        if (DEBUG.isDebugEnabled()) {
            DEBUG.debug("constructFilter returned filter: " + filter);
        }
        return filter;
    }

    protected Schema getSchema() throws IdRepoException {
        if (schema == null) {
            synchronized (this) {
                if (schema == null) {
                    try (Connection conn = createConnection()) {
                        schema = Schema.readSchemaForEntry(conn, Dn.valueOf(rootSuffix)).asStrictSchema();
                    } catch (LdapException ere) {
                        DEBUG.error("Unable to read the directory schema", ere);
                        throw new IdRepoException("Unable to read the directory schema");
                    }
                }
            }
        }
        return schema;
    }

    private void handleErrorResult(LdapException ere) throws IdRepoException {
        ResultCode resultCode = ere.getResult().getResultCode();
        String passwordPolicyErrorCode = helper.getPasswordPolicyErrorCode(ere.getResult());
        if (passwordPolicyErrorCode != null) {
            DEBUG.debug("Encountered password policy error : {}", ere.getResult().getDiagnosticMessageAsString());
            Object[] args = new Object[]{CLASS_NAME, resultCode.intValue(),
                    ere.getResult().getDiagnosticMessageAsString()};
            throw new PasswordPolicyException(resultCode, passwordPolicyErrorCode, args);
        }
        switch (resultCode.asEnum()) {
            case AUTHORIZATION_DENIED:
                DEBUG.debug("Encountered authorization error: {}", ere.getResult().getDiagnosticMessageAsString());
                throw newIdRepoException(resultCode, IdRepoErrorCode.PROXIED_AUTHZ_DENIED,
                        new Object[]{CLASS_NAME, resultCode.intValue(), ere.getResult().getDiagnosticMessage()});
            case CONSTRAINT_VIOLATION:
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.LDAP_EXCEPTION,
                        ResultCode.CONSTRAINT_VIOLATION, new Object[]{CLASS_NAME, resultCode.intValue(),
                        ere.getResult().getDiagnosticMessage()});
            case NO_SUCH_OBJECT:
                throw new IdentityNotFoundException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_FIND_ENTRY,
                        ResultCode.NO_SUCH_OBJECT, new Object[]{CLASS_NAME, ere.getResult().getDiagnosticMessage()});
            case SIZE_LIMIT_EXCEEDED:
                DEBUG.warn("Size limit exceeded.", ere);
                break;
            case TIME_LIMIT_EXCEEDED:
                DEBUG.warn("Time limit exceeded.", ere);
                break;
            default:
                throw newIdRepoException(resultCode, IdRepoErrorCode.LDAP_EXCEPTION_OCCURRED, CLASS_NAME,
                        resultCode.intValue());
        }
    }

    private IdRepoException newIdRepoException(String key, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, key, args);
    }

    private IdRepoException newIdRepoException(ResultCode resultCode, String key, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, key, String.valueOf(resultCode.intValue()), args);
    }

    private static class StringAttributeExtractor implements Function<Attribute, Set<String>, IdRepoException> {

        @Override
        public Set<String> apply(Attribute byteStrings) {
            return LDAPUtils.getAttributeValuesAsStringSet(byteStrings);
        }
    }

    private static class BinaryAttributeExtractor implements Function<Attribute, byte[][], IdRepoException> {
        @Override
        public byte[][] apply(Attribute byteStrings) {
            byte[][] values = new byte[byteStrings.size()][];
            int counter = 0;
            for (ByteString byteString : byteStrings) {
                byte[] bytes = byteString.toByteArray();
                values[counter++] = bytes;
            }
            return values;
        }
    }

    private static class StringToStringConverter implements Function<Map<String, Set<String>>, Map<String, Set<String>>,
            IdRepoException> {

        @Override
        public Map<String, Set<String>> apply(Map<String, Set<String>> stringSetMap) {
            return stringSetMap;
        }
    }

    private static class StringToBinaryConverter implements Function<Map<String, Set<String>>, Map<String, byte[][]>,
            IdRepoException> {

        @Override
        public Map<String, byte[][]> apply(Map<String, Set<String>> stringSetMap) {
            Map<String, byte[][]> result = new HashMap<>(stringSetMap.size());
            for (Map.Entry<String, Set<String>> entry : stringSetMap.entrySet()) {
                Set<String> values = entry.getValue();
                byte[][] binary = new byte[values.size()][];
                int counter = 0;
                for (String val : values) {
                    binary[counter++] = val.getBytes(Charset.forName("UTF-8"));
                }
                result.put(entry.getKey(), binary);
            }
            return result;
        }
    }

    private Connection createConnection() throws IdRepoException {
        try {
            return connectionFactory.create();
        } catch (DataLayerException e) {
            DEBUG.error("An error occurred while trying to create a connection to the datastore. {}",
                    getConnectionInformation(), e);
            throw newIdRepoException(IdRepoErrorCode.INITIALIZATION_ERROR, CLASS_NAME);
        }
    }

    private Connection createBindConnection() throws IdRepoException {
        try {
            return bindConnectionFactory.create();
        } catch (DataLayerException e) {
            DEBUG.error("An error occurred while trying to create a bind connection to the datastore", e);
            throw newIdRepoException(IdRepoErrorCode.INITIALIZATION_ERROR, CLASS_NAME);
        }
    }

    private Connection createPasswordConnection() throws IdRepoException {
        try {
            return passwordChangeConnectionFactory.create();
        } catch (DataLayerException e) {
            DEBUG.error("An error occurred while trying to create a password bind connection to the datastore", e);
            throw newIdRepoException(IdRepoErrorCode.INITIALIZATION_ERROR, CLASS_NAME);
        }
    }

    private String generateDNCacheKey(String name, IdType idType) {
        return name + "," + idType;
    }

    /**
     * Get the pSearchMap.
     *
     * @return the pSearchMap
     */
    @VisibleForTesting
    Map<String, DJLDAPv3PersistentSearch> getPsearchMap() {
        return PERSISTENT_SEARCH_MAP;
    }

    /**
     * Retrieve connection information for this instance of DJLDAPv3Repo so that it can be included in debug logging.
     * @return String containing server:port for each server in the connection string and root suffix.
     */
    private String getConnectionInformation() {
        StringBuilder sb = new StringBuilder();
        if (ldapServers != null) {
            sb.append("Servers:");
            for (LDAPURL ldapServer : ldapServers) {
                sb.append(" ");
                sb.append(ldapServer.toString());
            }
        }
        sb.append(", Root suffix: ");
        sb.append(rootSuffix);
        return sb.toString();
    }
}
