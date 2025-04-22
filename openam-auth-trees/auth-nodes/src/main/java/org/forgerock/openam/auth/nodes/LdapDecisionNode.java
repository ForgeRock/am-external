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
 * Copyright 2018-2025 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static javax.security.auth.callback.ConfirmationCallback.OK_CANCEL_OPTION;
import static javax.security.auth.callback.TextOutputCallback.ERROR;
import static javax.security.auth.callback.TextOutputCallback.INFORMATION;
import static javax.security.auth.callback.TextOutputCallback.WARNING;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.HeartbeatTimeUnit.SECONDS;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.LdapConnectionMode.LDAP;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.LdapConnectionMode.LDAPS;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.LdapConnectionMode.START_TLS;
import static org.forgerock.openam.ldap.LDAPConstants.STATUS_ACTIVE;
import static org.forgerock.openam.ldap.LDAPConstants.STATUS_INACTIVE;
import static org.forgerock.openam.ldap.ModuleState.CHANGE_AFTER_RESET;
import static org.forgerock.openam.ldap.ModuleState.PASSWORD_UPDATED_SUCCESSFULLY;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.apache.commons.lang.StringUtils;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.ldap.LDAPUtilException;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.ModuleState;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.SecretIdValidator;
import org.forgerock.opendj.ldap.Dn;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * A node that decides if the username and password exist in the LDAP database.
 * <p>
 * <p>Expects 'username' and 'password' fields to be present in the shared state.</p>
 */
@Node.Metadata(outcomeProvider = LdapDecisionNode.LdapOutcomeProvider.class,
        configClass = LdapDecisionNode.Config.class,
        tags = {"basic authn", "basic authentication"}, configValidator = LdapDecisionNode.ConfigValidator.class)
public class LdapDecisionNode implements Node {

    private static final String TRUE_OUTCOME_ID = "TRUE";
    private static final int OLD_PASSWORD_CALLBACK = 0;
    private static final int NEW_PASSWORD_CALLBACK = 1;
    private static final int CONFIRM_PASSWORD_CALLBACK = 2;
    private static final String LDAP_FLOW_STATE_KEY = "LdapFlowState";
    private static final Logger DEBUG = LoggerFactory.getLogger(LdapDecisionNode.class);
    private static final String BUNDLE = LdapDecisionNode.class.getName();
    private static final String USER_STATUS_ATTRIBUTE = "inetuserstatus";
    private static final String LAST_MODULE_STATE = "lastModuleState";
    private final Logger logger = LoggerFactory.getLogger(LdapDecisionNode.class);
    private final Config config;
    /** the CoreWrapper object for the Node. */
    protected final CoreWrapper coreWrapper;
    private final LegacyIdentityService identityService;
    private final Realm realm;
    private final Secrets secrets;
    private ResourceBundle bundle;
    private LDAPAuthUtils ldapUtil;
    private Set<String> additionalPwdChangeSearchAttrs;

    /**
     * The interface Config.
     */
    public interface Config {

        /**
         * Initialises the configuration if provided dynamically, {@link IdentityStoreDecisionNode}.
         *
         * @param context The Tree Context.
         * @throws NodeProcessException If there is a problem initialising the configuration.
         */
        default void initialise(TreeContext context) throws NodeProcessException { }

        /**
         * Primary LDAP server configuration.
         *
         * @return the set
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        Set<String> primaryServers();

        /**
         * Secondary LDAP server configuration.
         *
         * @return the set
         */
        @Attribute(order = 200)
        Set<String> secondaryServers();

        /**
         * Accounts search dn.
         *
         * @return the set
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        Set<String> accountSearchBaseDn();

        /**
         * Admin user dn.
         *
         * @return the string
         */
        @Attribute(order = 400)
        Optional<String> adminDn();

        /**
         * Admin user password.
         *
         * @return the char [ ]
         */
        @Attribute(order = 500)
        @Password
        Optional<char[]> adminPassword();

        /**
         * Attribute used for retrieving user's profile.
         *
         * @return the string
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        String userProfileAttribute();

        /**
         * Filter attributes used for searching a user to be authenticated.
         *
         * @return the set
         */
        @Attribute(order = 700, validators = {RequiredValueValidator.class})
        Set<String> searchFilterAttributes();

        /**
         * Custom search filter appended to the standard user search filter.
         *
         * @return the string
         */
        @Attribute(order = 800)
        Optional<String> userSearchFilter();

        /**
         * The level in the Directory Server that will be searched for a matching user profile.
         *
         * @return a {@link SearchScope} defining how the directory server will be searched.
         */
        @Attribute(order = 900, validators = {RequiredValueValidator.class})
        default SearchScope searchScope() {
            return SearchScope.SUBTREE;
        }

        /**
         * Specifies the LDAP connection mode.
         *
         * @return a {@link LdapConnectionMode} defining the connection mode.
         */
        @Attribute(order = 1000, validators = {RequiredValueValidator.class})
        default LdapConnectionMode ldapConnectionMode() {
            return LDAP;
        }

        /**
         * Indicates if mTLS is enabled.
         *
         * @return true if mTLS is enabled otherwise false.
         */
        @Attribute(order = 1033, validators = {RequiredValueValidator.class})
        default boolean mtlsEnabled() {
            return false;
        }

        /**
         * Label used for mapping to the mTLS certificate in the secret store.
         *
         * @return the label
         */
        @Attribute(order = 1066, validators = {SecretIdValidator.class})
        Optional<String> mtlsSecretLabel();

        /**
         * Controls whether the DN or the username is returned as the authentication principal.
         *
         * @return <code>true</code> to return the DN.
         */
        @Attribute(order = 1100, validators = {RequiredValueValidator.class})
        default boolean returnUserDn() {
            return true;
        }

        /**
         * Controls the mapping of local attribute to external attribute for dynamic profile creation.
         *
         * @return A set of string attribute names.
         */
        @Attribute(order = 1200)
        Set<String> userCreationAttrs();

        /**
         * Enforced when the user is resetting their password as part of the authentication.
         *
         * @return int representing the minimum size for a user password.
         */
        @Attribute(order = 1300, validators = {RequiredValueValidator.class})
        default int minimumPasswordLength() {
            return 8;
        }

        /**
         * Enables support for modern LDAP password policies.
         *
         * @return <code>true</code> to enabled behera support.
         */
        @Attribute(order = 1400, validators = {RequiredValueValidator.class})
        default boolean beheraEnabled() {
            return true;
        }

        /**
         * Enables a X509TrustManager that trusts all certificates.
         *
         * @return <code>true</code> to enable X509TrustManager.
         */
        @Attribute(order = 1500, validators = {RequiredValueValidator.class})
        default boolean trustAllServerCertificates() {
            return false;
        }

        /**
         * Specifies how often should OpenAM send a heartbeat request to the directory.
         *
         * @return <code>int</code> representing the heartbeat interval.
         */
        @Attribute(order = 1600, validators = {RequiredValueValidator.class})
        default int heartbeatInterval() {
            return 10;
        }

        /**
         * Defines the time unit corresponding to the Heartbeat Interval setting.
         *
         * @return a {@link HeartbeatTimeUnit} denoting the unit of time for the heartbeat interval setting.
         */
        @Attribute(order = 1700, validators = {RequiredValueValidator.class})
        default HeartbeatTimeUnit heartbeatTimeUnit() {
            return SECONDS;
        }

        /**
         * Defines the timeout in seconds OpenAM should wait for a response of the Directory Server. 0 means no timeout.
         *
         * @return <code>int</code> representing the response timeout.
         */
        @Attribute(order = 1800, validators = {RequiredValueValidator.class})
        default int ldapOperationsTimeout() {
            return 0;
        }

        /**
         * Filter attributes to be applied in addition to the {@link Config#searchFilterAttributes} when searching for a
         * user during a password change. The username to search for when processing a password change comes from shared
         * state and the filter attributes applied during authentication may not provide a match, especially when
         * {@link Config#returnUserDn()} is {@code true}.
         *
         * @return the set of additional user profile attributes to search for.
         */
        default Set<String> additionalPasswordChangeSearchAttributes() {
            return emptySet();
        }

        /**
         * Determines whether mixed case should be used for password change messages returned from this node.
         * Previously all messages were returned uppercase.
         *
         * @return <code>true</code> to enable the use of mixed case messages.
         */
        @Attribute(order = 1900)
        default boolean mixedCaseForPasswordChangeMessages() {
            return false;
        }
    }

    /**
     * Validator that determines that the configuration is valid for mTLS and no mTLS connection modes.
     * <p>
     * <br>
     *     When mTLS is enabled:
     *     <ul>
     *         <li>Connection mode must be LDAPS</li>
     *         <li>Secret label must be provided</li>
     *     </ul>
     *     When mTLS is disabled:
     *     <ul>
     *         <li>Admin dn must be provided</li>
     *         <li>Admin password must be provided</li>
     *     </ul>
     * </p>
     */
    static class ConfigValidator implements ServiceConfigValidator {

        private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);

        public static final String ADMIN_DN_METHOD_NAME = "adminDn";
        public static final String ADMIN_PASSWORD_METHOD_NAME = "adminPassword";
        public static final String LDAP_CONNECTION_MODE_METHOD_NAME = "ldapConnectionMode";
        public static final String MTLS_ENABLED_METHOD_NAME = "mtlsEnabled";
        public static final String MTLS_SECRET_LABEL_METHOD_NAME = "mtlsSecretLabel";

        private final AnnotatedServiceRegistry serviceRegistry;

        @Inject
        ConfigValidator(AnnotatedServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        public void validate(final Realm realm, final List<String> configPath,
                final Map<String, Set<String>> attributes) throws ServiceConfigException, ServiceErrorException {
            Reject.ifNull(configPath, "The configuration path is required for validation.");
            Reject.ifTrue(configPath.isEmpty(), "The configuration path is required for validation.");
            Reject.ifNull(realm, "The realm is required for validation.");
            Reject.ifNull(attributes, "Attributes are required for validation");
            String nodeId = configPath.get(configPath.size() - 1);
            Optional<LdapDecisionNode.Config> possibleExistingConfig;
            try {
                possibleExistingConfig = serviceRegistry.getRealmInstance(LdapDecisionNode.Config.class, realm, nodeId);
            } catch (SSOException | SMSException e) {
                logger.error("An error occurred validating LDAP decision node configuration", e);
                throw new ServiceErrorException("Error validating LDAP decision node configuration");
            }
            String adminUsername = getConfigValue(ADMIN_DN_METHOD_NAME, config -> config.adminDn().orElse(null),
                    possibleExistingConfig, attributes);
            String adminPassword = getConfigValue(ADMIN_PASSWORD_METHOD_NAME,
                config -> config.adminPassword().orElse(null), possibleExistingConfig, attributes);
            String ldapConnectionMode = getConfigValue(LDAP_CONNECTION_MODE_METHOD_NAME, Config::ldapConnectionMode,
                    possibleExistingConfig, attributes);
            String mtlsEnabled = getConfigValue(MTLS_ENABLED_METHOD_NAME, Config::mtlsEnabled, possibleExistingConfig,
                    attributes);
            String mtlsSecretLabel = getConfigValue(MTLS_SECRET_LABEL_METHOD_NAME,
                config -> config.mtlsSecretLabel().orElse(null), possibleExistingConfig, attributes);

            if (Boolean.parseBoolean(mtlsEnabled)) {
                if (!LDAPS.name().equals(ldapConnectionMode)) {
                    throw new ServiceConfigException("When mTLS is enabled the LDAP Connection Mode must be LDAPS");
                }
                if (isBlank(mtlsSecretLabel)) {
                    throw new ServiceConfigException("When mTLS is enabled the mTLS Secret Label must be specified");
                }
            } else {
                if (isBlank(adminUsername)) {
                    throw new ServiceConfigException("When mTLS is disabled the Bind User DN must be provided");
                }
                if (isBlank(adminPassword)) {
                    throw new ServiceConfigException("When mTLS is disabled the Bind User Password must be provided");
                }
            }
        }

        private String getConfigValue(String attribute, Function<Config, ?> attributeMethod,
                Optional<Config> existingConfig, Map<String, Set<String>> attributes) {
            if (attributes.containsKey(attribute)) {
                return getFirstItem(attributes.get(attribute));
            }
            Object value = existingConfig.map(attributeMethod).orElse(null);
            return value == null ? null : String.valueOf(value);
        }
    }

    /**
     * Constructs a new {@link LdapDecisionNode} with the provided {@link Config}.
     *
     * @param config          provides the settings for initialising an {@link LdapDecisionNode}.
     * @param realm           The current realm.
     * @param coreWrapper     A core wrapper instance.
     * @param identityService An {@link LegacyIdentityService} instance.
     * @param secrets         A Secrets instance.
     */
    @Inject
    public LdapDecisionNode(@Assisted Config config, @Assisted Realm realm, CoreWrapper coreWrapper,
            LegacyIdentityService identityService, Secrets secrets) {
        this.config = config;
        this.realm = realm;
        this.coreWrapper = coreWrapper;
        this.identityService = identityService;
        this.secrets = secrets;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("LdapDecisionNode started");
        try {
            config.initialise(context);
        } catch (NodeProcessException e) {
            logger.debug("Failed to initialise LDAP configuration. All executions will have the 'FALSE' outcome.", e);
            return goTo(LdapOutcome.FALSE).build();
        }
        ActionBuilder action;
        JsonValue newState = context.sharedState.copy();
        String sharedStateSubmittedUsername = context.sharedState.get(USERNAME).asString();
        String realm = context.sharedState.get(REALM).asString();
        String username = sharedStateSubmittedUsername;
        try {
            ldapUtil = initializeLDAP(context);
            String userPassword = context.getState(SharedStateConstants.PASSWORD).asString();
            if (isLogin(context)) {
                logger.debug("processing login");
                ldapUtil.setDynamicProfileCreationEnabled(true);
                ldapUtil.setUserAttributes(ImmutableSet.of(USER_STATUS_ATTRIBUTE));
                String userStatus = STATUS_INACTIVE;
                if (authenticateUser(ldapUtil, sharedStateSubmittedUsername, userPassword)) {
                    userStatus = STATUS_ACTIVE;
                    if (ldapUtil.getUserAttributeValues().containsKey(USER_STATUS_ATTRIBUTE)) {
                        userStatus = ldapUtil.getUserAttributeValues().get(USER_STATUS_ATTRIBUTE).iterator().next();
                    }
                }
                username = getUserNameFromIdentity(ldapUtil.getUserId());
                if (StringUtils.isNotEmpty(username) && isUsernameRepresentedByUniversalId()) {
                    newState.put(USERNAME, username);
                }
                if (!userStatus.equalsIgnoreCase(STATUS_ACTIVE)) {
                    ldapUtil.setState(ModuleState.ACCOUNT_LOCKED);
                }
                action = processLogin(ldapUtil.getState(), newState, context);
            } else {
                logger.debug("processing password change");
                action = processPasswordChange(context, sharedStateSubmittedUsername, userPassword);
            }
        } catch (LDAPUtilException e) {
            logger.error(e.getMessage(), e);
            ResourceBundle bundle = getBundleInPreferredLocale(context);
            if (e.getResultCode() == null) {
                logger.warn("Invalid configuration");
                throw new NodeProcessException(bundle.getString("InvalidConfiguration"));
            } else if (e.getResultCode().equals(ResultCode.UNWILLING_TO_PERFORM)) {
                logger.warn("Server error");
                throw new NodeProcessException(bundle.getString("ServerError"));
            } else {
                String userLockedStatus = getFirstItem(
                        ldapUtil.getUserAttributeValues().get(USER_STATUS_ATTRIBUTE));
                if (StringUtils.isNotEmpty(userLockedStatus) && !userLockedStatus.equalsIgnoreCase(STATUS_ACTIVE)) {
                    action = goTo(LdapOutcome.LOCKED).withLockoutMessage(bundle.getString("accountLocked"));
                } else {
                    action = goTo(LdapOutcome.FALSE).withErrorMessage(bundle.getString("authenticationFailed"));
                }
            }
        }
        return action
                .withUniversalId(identityService.getUniversalId(username, realm, USER))
                .replaceSharedState(newState).build();
    }

    /**
     * Allows for the username to be represented by the user's universal id.
     *
     * @return {@code true} if the username should be represented by the universal id value, otherwise {@code false}
     */
    protected boolean isUsernameRepresentedByUniversalId() {
        return true;
    }

    /**
     * Authenticate user using provided credentials.
     *
     * @param ldapUtil the LDAPAuthUtils context
     * @param userName
     * @param userPassword
     * @return true if authenticates,
     *         false if LDAP server unwilling to authenticate
     * @throws LDAPUtilException if there is other LDAP exceptions
     */
    private boolean authenticateUser(LDAPAuthUtils ldapUtil, String userName, String userPassword)
            throws LDAPUtilException {
        if (isBlank(userName) || isBlank(userPassword)) {
            throw new LDAPUtilException("CredInvalid", ResultCode.INVALID_CREDENTIALS, null);
        }
        try {
            ldapUtil.authenticateUser(userName, userPassword);
            return true;
        } catch (LDAPUtilException ex) {
            if (ResultCode.UNWILLING_TO_PERFORM.equals(ex.getResultCode())) {
                logger.debug("LDAP Unwilling to perform authenticate on user {}.", userName);
                return false;
            }
            throw ex;
        }
    }

    private ActionBuilder processPasswordChange(TreeContext context, String userName, String userPassword)
            throws LDAPUtilException, NodeProcessException {
        ActionBuilder action;
        logger.debug("In the Password Change screen.");
        ldapUtil.setUserId(userName);
        ldapUtil.setUserPassword(userPassword);
        ldapUtil.searchForUser(additionalPwdChangeSearchAttrs);
        if (userClickedSubmit(context)) {
            List<PasswordCallback> passwordCallbacks = context.getCallbacks(PasswordCallback.class);
            char[] oldPassword = getPasswordFromCallback(passwordCallbacks.get(OLD_PASSWORD_CALLBACK));
            char[] newPassword = getPasswordFromCallback(passwordCallbacks.get(NEW_PASSWORD_CALLBACK));
            char[] confirmPassword = getPasswordFromCallback(passwordCallbacks.get(CONFIRM_PASSWORD_CALLBACK));

            ModuleState passwordChangeState;
            if (newPassword.length < config.minimumPasswordLength()) {
                logger.debug("LDAP.process: new password less"
                            + " than the minimal length of {}", config.minimumPasswordLength());
                passwordChangeState = ModuleState.PASSWORD_MIN_CHARACTERS;
            } else {
                if (ldapUtil.getUserDN() == null) {
                    logger.warn("Attempt to modify password when userDN not found");

                    // ensure a NodeProcessException is thrown rather than going to the failure outcome which indicates
                    // that authentication has failed. This is thrown with an InvalidConfiguration message as this could
                    // be resolved by changing configuration - see OPENAM-18775
                    throw new NodeProcessException(getBundleInPreferredLocale(context)
                        .getString("InvalidConfiguration"));
                }

                ldapUtil.changePassword(oldPassword, newPassword, confirmPassword);
                passwordChangeState = ldapUtil.getState();
            }
            logger.debug("Password change state :{}", passwordChangeState);
            action = processPasswordChange(passwordChangeState);
            if (passwordChangeState == PASSWORD_UPDATED_SUCCESSFULLY) {
                action.replaceTransientState(context.transientState.copy().put(PASSWORD, newPassword));
            }
        } else {
            if (userPasswordHasBeenReset(context)) {
                action = goTo(LdapOutcome.CANCELLED);
            } else {
                action = goTo(LdapOutcome.TRUE);
            }
        }
        return action;
    }

    private boolean userPasswordHasBeenReset(TreeContext context) {
        if (context.sharedState.isDefined(LAST_MODULE_STATE)) {
            String lastModuleState = context.sharedState.get(LAST_MODULE_STATE).asString();
            return ModuleState.valueOf(lastModuleState) == CHANGE_AFTER_RESET;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the context is currently at the login stage, as opposed to the Password Change stage.
     *
     * @param context represents the context of the current authentication tree.
     * @return true if we are at the login stage,
     */
    private boolean isLogin(TreeContext context) {
        return !context.hasCallbacks();
    }

    private ActionBuilder goTo(LdapOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * Initialize ldap.
     *
     * @param context the context
     * @throws NodeProcessException the node process exception
     */
    private LDAPAuthUtils initializeLDAP(TreeContext context) throws NodeProcessException {
        logger.debug("LDAP initialize()");
        bundle = context.request.locales
                .getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        LDAPAuthUtils ldapUtil;
        try {
            org.forgerock.opendj.ldap.SearchScope searchScope = config.searchScope().asLdapSearchScope();
            boolean useStartTLS = config.ldapConnectionMode() == START_TLS;
            boolean isSecure = config.ldapConnectionMode() == LDAPS || useStartTLS;
            String baseDn = config.accountSearchBaseDn().stream()
                    .collect(Collectors.joining(","));
            ldapUtil = coreWrapper.getLDAPAuthUtils(config.primaryServers(), config.secondaryServers(),
                    isSecure, bundle, baseDn, DEBUG);
            ldapUtil.setScope(searchScope);
            if (config.userSearchFilter().isPresent()) {
                ldapUtil.setFilter(config.userSearchFilter().get());
            }
            ldapUtil.setUserNamingAttribute(config.userProfileAttribute());
            ldapUtil.setUserSearchAttribute(config.searchFilterAttributes());
            if (config.adminPassword().isPresent()) {
                ldapUtil.setAuthPassword(config.adminPassword().get());
            }
            if (config.adminDn().isPresent()) {
                ldapUtil.setAuthDN(config.adminDn().get());
            }
            ldapUtil.setMtlsEnabled(config.mtlsEnabled());
            if (config.mtlsSecretLabel().isPresent()) {
                ldapUtil.setMtlsSecretId(
                        format(getMtlsSecretIdTemplate(), config.mtlsSecretLabel().get()));
            }
            ldapUtil.setRealm(realm);
            ldapUtil.setSecrets(secrets);
            ldapUtil.setReturnUserDN(config.returnUserDn());
            ldapUtil.setUserAttributes(config.userCreationAttrs());
            ldapUtil.setTrustAll(config.trustAllServerCertificates());
            ldapUtil.setUseStartTLS(useStartTLS);
            //TODO The work for this is dependent on https://bugster.forgerock.org/jira/browse/AME-15017 being
            // completed first, as it requires access to the user profile.
            ldapUtil.setDynamicProfileCreationEnabled(false);
            ldapUtil.setBeheraEnabled(config.beheraEnabled());
            ldapUtil.setHeartBeatInterval(config.heartbeatInterval());
            ldapUtil.setHeartBeatTimeUnit(config.heartbeatTimeUnit().toString());
            ldapUtil.setOperationTimeout(config.ldapOperationsTimeout());
            additionalPwdChangeSearchAttrs = config.additionalPasswordChangeSearchAttributes();

            logger.debug("bindDN-> " + config.returnUserDn()
                                   + "\nrequiredPasswordLength-> " + config.minimumPasswordLength()
                                   + "\nbaseDN-> " + config.adminDn().orElse("")
                                   + "\nmtlsEnabled-> " + config.mtlsEnabled()
                                   + "\nmtlsSecretLabel->" + config.mtlsSecretLabel().orElse("")
                                   + "\nuserNamingAttr-> " + config.userProfileAttribute()
                                   + "\nuserSearchAttr(s)-> " + config.searchFilterAttributes()
                                   + "\nuserCreationAttrs-> " + config.userCreationAttrs()
                                   + "\nsearchFilter-> " + config.userSearchFilter()
                                   + "\nsearchScope-> " + searchScope
                                   + "\nisSecure-> " + isSecure
                                   + "\nuseStartTLS-> " + useStartTLS
                                   + "\ntrustAll-> " + config.trustAllServerCertificates()
                                   + "\nbeheraEnabled->" + config.beheraEnabled()
                                   + "\nprimaryServers-> " + config.primaryServers()
                                   + "\nsecondaryServers-> " + config.secondaryServers()
                                   + "\nheartBeatInterval-> " + config.heartbeatInterval()
                                   + "\nheartBeatTimeUnit-> " + config.heartbeatTimeUnit()
                                   + "\noperationTimeout-> " + config.ldapOperationsTimeout()
                                   + "\nadditionalPasswordChangeSearchAttributes-> " + additionalPwdChangeSearchAttrs);
        } catch (LDAPUtilException e) {
            logger.warn("Init Exception");
            throw new NodeProcessException(bundle.getString("NoServer"), e);
        }
        return ldapUtil;
    }

    /**
     * Get a mTLS secret label string ready to be formatted.
     *
     * @return the mTLS secretId string.
     */
    protected String getMtlsSecretIdTemplate() {
        return Labels.LDAP_DECISION_NODE_MTLS_CERT;
    }

    private boolean userClickedSubmit(TreeContext context) {
        return context.getCallback(ConfirmationCallback.class).get().getSelectedIndex() == 0;
    }

    private char[] getPasswordFromCallback(PasswordCallback callback) {
        char[] password = callback.getPassword() == null ? new char[0] : callback.getPassword();
        callback.clearPassword();
        return password;
    }

    private ActionBuilder processLogin(ModuleState loginState, JsonValue newState, TreeContext context)
            throws NodeProcessException {
        ActionBuilder loginResult = goTo(LdapOutcome.TRUE);
        String username = newState.get(USERNAME).asString();
        if (username != null) {
            loginResult.withIdentifiedIdentity(username, USER);
        }
        ResourceBundle bundle = context.request.locales
                .getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        logger.debug("loginState {}", loginState);
        switch (loginState) {
        case SUCCESS:
            break;
        case PASSWORD_EXPIRING:
            loginResult = Action.send(passwordChangeCallbacks(WARNING,
                    Locale.formatMessage(bundle.getString("PasswordExpiring"), ldapUtil.getExpTime())));
            context.sharedState.put(LDAP_FLOW_STATE_KEY, LdapFlowState.PASSWORD_CHANGE);
            break;
        case PASSWORD_RESET_STATE:
        case CHANGE_AFTER_RESET:
            newState.put(LAST_MODULE_STATE, CHANGE_AFTER_RESET);
            loginResult = Action
                    .send(passwordChangeCallbacks(INFORMATION, bundle.getString("PasswordReset")));
            break;
        case PASSWORD_EXPIRED_STATE:
            logger.debug("Password for user {} has expired.", ldapUtil.getUserId());
            loginResult = goTo(LdapOutcome.EXPIRED);
            break;
        case ACCOUNT_LOCKED:
            logger.debug("Account for user {} has been locked.", ldapUtil.getUserId());
            loginResult = goTo(LdapOutcome.LOCKED)
                    .withLockoutMessage(bundle.getString("accountLocked"))
                    .withErrorMessage(bundle.getString("accountLocked"));
            break;
        case GRACE_LOGINS:
            String message = Locale.formatMessage(bundle.getString("GraceLogins"), ldapUtil.getGraceLogins());
            List<Callback> callbacks = ldapUtil.getGraceLogins() == 1
                    ? passwordChangeCallbacks(INFORMATION, message,
                                              new ConfirmationCallback(
                                                      OK_CANCEL_OPTION,
                                                      new String[]{bundle.getString("confirmationSubmit")},
                                                      0))
                    : passwordChangeCallbacks(INFORMATION, message);
            loginResult = Action.send(callbacks);
            break;
        case USER_NOT_FOUND:
            loginResult = goTo(LdapOutcome.FALSE).withErrorMessage(bundle.getString("authenticationFailed"));
            break;
        default:
            logger.warn("Unknown login state");
            throw new NodeProcessException(
                    format("Encountered an unknown state '%s' during authentication.", loginState));
        }
        return loginResult.replaceSharedState(newState);
    }

    private ActionBuilder processPasswordChange(ModuleState passwordState)
            throws NodeProcessException {
        ActionBuilder passwordChangeResult = goTo(LdapOutcome.TRUE);
        logger.debug("passwordState {}", passwordState);
        switch (passwordState) {
        case ACCOUNT_LOCKED:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("AcctInactive")));
            break;
        case PASSWORD_UPDATED_SUCCESSFULLY:
            break;
        case PASSWORD_NOT_UPDATE:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("PInvalid")));
            break;
        case PASSWORD_MISMATCH:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("PasswdMismatch")));
            break;
        case WRONG_PASSWORD_ENTERED:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("PasswdSame")));
            break;
        case PASSWORD_MIN_CHARACTERS:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("pwdTooShort")));
            break;
        case USER_PASSWORD_SAME:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("UPsame")));
            break;
        case INSUFFICIENT_PASSWORD_QUALITY:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("inPwdQual")));
            break;
        case PASSWORD_IN_HISTORY:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("pwdInHist")));
            break;
        case PASSWORD_TOO_SHORT:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("pwdTooShort")));
            break;
        case PASSWORD_TOO_YOUNG:
            passwordChangeResult = Action
                    .send(passwordChangeCallbacks(ERROR, bundle.getString("pwdTooYoung")));
            break;
        default:
            logger.warn("unknown passwordState");
            throw new NodeProcessException(format("Encountered an unknown state '%s' during password change.",
                    passwordState));
        }
        return passwordChangeResult;
    }

    /**
     * These are the states that the Node can find itself in and entail a specific screen presented in the UI.
     */
    private enum LdapFlowState {
        /**
         * Password change ldap flow state.
         */
        PASSWORD_CHANGE,
        /**
         * Password expired ldap flow state.
         */
        PASSWORD_EXPIRED,
        /**
         * Password expiring not updated ldap flow state.
         */
        PASSWORD_EXPIRING_NOT_UPDATED,
        /**
         * User inactive ldap flow state.
         */
        USER_INACTIVE,
        /**
         * Account locked ldap flow state.
         */
        ACCOUNT_LOCKED
    }

    private List<Callback> passwordChangeCallbacks(int messageType, String message) {
        return passwordChangeCallbacks(messageType,
                                       message,
                                       new ConfirmationCallback(OK_CANCEL_OPTION,
                                                                new String[]{bundle.getString("confirmationSubmit"),
                                                                        bundle.getString("confirmationCancel")},
                                                                1));
    }

    private List<Callback> passwordChangeCallbacks(int messageType, String message,
                                                   ConfirmationCallback confirmationCallback) {
        return ImmutableList.of(
                new TextOutputCallback(messageType, (
                        config.mixedCaseForPasswordChangeMessages() ? message : message.toUpperCase())),
                new PasswordCallback(bundle.getString("oldPasswordCallback"), false),
                new PasswordCallback(bundle.getString("newPasswordCallback"), false),
                new PasswordCallback(bundle.getString("confirmPasswordCallback"), false),
                confirmationCallback
        );
    }

    /**
     * Used to control how an LDAP server is searched.
     */
    public enum SearchScope {
        /**
         * Only the base DN is searched.
         */
        OBJECT(org.forgerock.opendj.ldap.SearchScope.BASE_OBJECT),
        /**
         * Only the single level below (and not the Base DN) is searched.
         */
        ONE_LEVEL(org.forgerock.opendj.ldap.SearchScope.SINGLE_LEVEL),
        /**
         * The Base DN and all levels below are searched.
         */
        SUBTREE(org.forgerock.opendj.ldap.SearchScope.WHOLE_SUBTREE);

        SearchScope(org.forgerock.opendj.ldap.SearchScope searchScope) {
            this.searchScope = searchScope;
        }

        final org.forgerock.opendj.ldap.SearchScope searchScope;

        private org.forgerock.opendj.ldap.SearchScope asLdapSearchScope() {
            return searchScope;
        }
    }

    /**
     * Defines which protocol/operation is used to establish the connection to the LDAP Directory Server.
     */
    public enum LdapConnectionMode {
        /**
         * The connection won't be secured and passwords are transferred in cleartext over the network.
         */
        LDAP,
        /**
         * the connection is secured via SSL or TLS.
         */
        LDAPS,
        /**
         * the connection is secured by using StartTLS extended operation.
         */
        START_TLS
    }

    /**
     * Units used by the heartbeat time interval setting.
     */
    public enum HeartbeatTimeUnit {
        /**
         * Seconds heartbeat time unit.
         */
        SECONDS,
        /**
         * Minute heartbeat time unit.
         */
        MINUTES,
        /**
         * Hour heartbeat time unit.
         */
        HOURS
    }

    /**
     * The possible outcomes for the LdapDecisionNode.
     */
    public enum LdapOutcome {
        /**
         * Successful authentication.
         */
        TRUE,
        /**
         * Authentication failed.
         */
        FALSE,
        /**
         * The ldap user account has been locked.
         */
        LOCKED,
        /**
         * The ldap user's password has expired.
         */
        EXPIRED,
        /**
         * The user clicked the cancel button on the password change screen when either force-change-on-add or
         * force-change-on-reset are 'true'.
         */
        CANCELLED
    }

    /**
     * Defines the possible outcomes from this Ldap node.
     */
    public static class LdapOutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(LdapDecisionNode.BUNDLE,
                    LdapOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(LdapOutcome.TRUE.name(), bundle.getString("trueOutcome")),
                    new Outcome(LdapOutcome.FALSE.name(), bundle.getString("falseOutcome")),
                    new Outcome(LdapOutcome.LOCKED.name(), bundle.getString("lockedOutcome")),
                    new Outcome(LdapOutcome.CANCELLED.name(), bundle.getString("cancelledOutcome")),
                    new Outcome(LdapOutcome.EXPIRED.name(), bundle.getString("expiredOutcome")));
        }
    }

    private String getUserNameFromIdentity(String identity) {
        if (identity == null) {
            return null;
        }

        if (!config.returnUserDn()) {
            return identity;
        }

        return LDAPUtils.getName(Dn.valueOf(identity));
    }

    private ResourceBundle getBundleInPreferredLocale(TreeContext context) {
        return context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(USERNAME),
            new InputState(PASSWORD)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(USERNAME, singletonMap(TRUE_OUTCOME_ID, false))
        };
    }
}
