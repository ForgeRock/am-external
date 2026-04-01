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
 * Copyright 2025-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapper;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapperFactory;
import org.forgerock.openam.ldap.LDAPUtilException;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

import static com.sun.identity.idm.IdType.USER;
import static java.lang.String.format;
import static javax.security.auth.callback.TextOutputCallback.INFORMATION;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.ldap.ModuleState.CHANGE_AFTER_RESET;

/**
 * A node that decides if the username and password exist in an Active Directory.
 * Expects 'username' and 'password' fields to be present in the shared state.
 * Includes outcomes for AD specific states such as account expired, account locked and account disabled.
 */
@Node.Metadata(outcomeProvider = ADDecisionNode.ADOutcomeProvider.class, configClass = ADDecisionNode.Config.class,
        tags = { "basic authn", "basic authentication" }, configValidator = LdapDecisionNode.ConfigValidator.class)
public class ADDecisionNode extends LdapDecisionNode {

    private static final String BUNDLE = ADDecisionNode.class.getName();
    private final Logger logger = LoggerFactory.getLogger(ADDecisionNode.class);
    private final Config config;

    /**
     * The interface Config.
     */
    public interface Config extends LdapDecisionNode.Config {

        /**
         * Behera algorithm is not supported for AD. Therefore, overriding it and hiding it from the UI.
         *
         * @return false
         */
        @Override
        default boolean beheraEnabled() {
            return false;
        }

        /**
         * Controls what failure message to return when the account is locked, an authentication failure message
         * (when value is false) or an account locked message (which will be returned whether a correct or incorrect
         * password is provided).
         *
         * @return <code>true</code> to return the account locked message, <code>false</code> authentication failure.
         */
        @Attribute(order = 1099)
        default boolean returnAccountLockedMessage() {
            return false;
        }

    }

    /**
     * Constructs a new Active Directory Decision Node.
     *
     * @param config                               The config extended from LDAPDecisionNode.
     * @param realm                                The realm.
     * @param coreWrapper                          A core wrapper instance.
     * @param identityService                      An IdentityService instance.
     * @param secrets                              A Secrets instance.
     * @param connectionFactoryAuditWrapperFactory A factory for creating {@link ConnectionFactoryAuditWrapper}.
     * @param accountLockoutTreesFactory           A factory for creating {@link AMAccountLockoutTrees}.
     */
    @Inject
    public ADDecisionNode(@Assisted Config config, @Assisted Realm realm, CoreWrapper coreWrapper,
                          LegacyIdentityService identityService, Secrets secrets,
                          ConnectionFactoryAuditWrapperFactory connectionFactoryAuditWrapperFactory,
                          AMAccountLockoutTrees.Factory accountLockoutTreesFactory) {
        super(config, realm, coreWrapper, identityService, secrets, connectionFactoryAuditWrapperFactory,
                accountLockoutTreesFactory);
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ADDecisionNode started");
        try {
            config.initialise(context);
        } catch (NodeProcessException e) {
            logger.debug("Failed to initialise AD configuration. All executions will have the 'FALSE' outcome.", e);
            return goTo(LdapOutcome.FALSE).build();
        }
        Action.ActionBuilder action;
        NodeState nodeState = context.getStateFor(this);
        String sharedStateSubmittedUsername = nodeState.get(USERNAME).asString();
        String username = sharedStateSubmittedUsername;
        try {
            ldapUtil = initializeLDAP(context, true);
            String userPassword = nodeState.get(SharedStateConstants.PASSWORD).asString();
            if (isLogin(context)) {
                ldapUtil.setDynamicProfileCreationEnabled(true);
                authenticateUser(ldapUtil, sharedStateSubmittedUsername, userPassword);
                String usernameFromIdentity = getUserNameFromIdentity(ldapUtil.getUserId());
                if (StringUtils.isNotEmpty(usernameFromIdentity) && isUsernameRepresentedByUniversalId()) {
                    username = usernameFromIdentity;
                }
                Action.ActionBuilder loginResult = goTo(LdapOutcome.TRUE);
                if (username != null) {
                    loginResult.withIdentifiedIdentity(username, USER);
                }
                action = updateLoginResult(context, nodeState, loginResult);
            } else {
                logger.debug("Processing password change");
                action = processPasswordChange(context, sharedStateSubmittedUsername, userPassword);
            }
        } catch (LDAPUtilException e) {
            ResultCode resultCode = e.getResultCode();
            String message = e.getMessage();

            if (ResultCode.INVALID_CREDENTIALS.equals(resultCode)) {
                logger.info("LDAPUtilException: {}", message);
            } else {
                logger.error("LDAPUtilException: {}", message);
            }

            logger.debug("Exception", e);
            ResourceBundle bundle = getBundleInPreferredLocale(context);

            if (resultCode == null) {
                logger.warn("Invalid configuration");
                throw new NodeProcessException(bundle.getString("InvalidConfiguration"));
            }

            if (ResultCode.UNWILLING_TO_PERFORM.equals(resultCode)) {
                logger.warn("Server error");
                throw new NodeProcessException(bundle.getString("ServerError"));
            }

            action = goTo(LdapOutcome.FALSE).withErrorMessage(bundle.getString("authenticationFailed"));
        }
        return action.build();
    }


    /** Updates the login result based on the state of the LDAPUtil after an authentication attempt.
     *
     * @param context     The tree context.
     * @param nodeState   The node state.
     * @param loginResult The current login result action builder.
     * @return The updated login result action builder.
     * @throws NodeProcessException If an unknown state is encountered.
     */
    private Action.ActionBuilder updateLoginResult(TreeContext context, NodeState nodeState,
                                                   Action.ActionBuilder loginResult) throws NodeProcessException {
        ResourceBundle bundle = context.request.locales
                .getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        switch (ldapUtil.getState()) {
        case SUCCESS:
            break;
        case PASSWORD_RESET_STATE:
        case CHANGE_AFTER_RESET:
            nodeState.putShared(LAST_MODULE_STATE, CHANGE_AFTER_RESET);
            loginResult = Action
                    .send(passwordChangeCallbacks(INFORMATION, bundle.getString("pwdResetReq")));
            break;
        case PASSWORD_EXPIRED_STATE:
            logger.debug("Password for user {} has expired.", ldapUtil.getUserId());
            loginResult = goTo(LdapOutcome.PWD_EXPIRED).withErrorMessage(bundle.getString("pwdExpired"));
            break;
        case ACCOUNT_DISABLED:
            logger.debug("Account for user {} has been disabled.", ldapUtil.getUserId());
            loginResult = goTo(LdapOutcome.ACT_DISABLED)
                    .withLockoutMessage(bundle.getString("actDisabled"))
                    .withErrorMessage(bundle.getString("actDisabled"));
            break;
        case ACCOUNT_LOCKED:
            logger.debug("Account for user {} has been locked.", ldapUtil.getUserId());
            loginResult = config.returnAccountLockedMessage() ? goTo(LdapOutcome.ACT_LOCKED)
                    .withLockoutMessage(bundle.getString("actLocked"))
                    .withErrorMessage(bundle.getString("actLocked"))
                    : goTo(LdapOutcome.ACT_LOCKED).withLockoutMessage(bundle.getString("authenticationFailed"))
                            .withErrorMessage(bundle.getString("authenticationFailed"));
            break;
        case ACCOUNT_EXPIRED:
            logger.debug("Account for user {} has expired.", ldapUtil.getUserId());
            loginResult = goTo(LdapOutcome.ACT_EXPIRED)
                    .withErrorMessage(bundle.getString("actExpired"));
            break;
        case USER_NOT_FOUND:
            loginResult = goTo(LdapOutcome.FALSE)
                    .withErrorMessage(bundle.getString("authenticationFailed"));
            break;
        default:
            logger.warn("Unknown login state");
            throw new NodeProcessException(
                    format("Encountered an unknown state '%s' during authentication.", ldapUtil.getState()));
        }
        return loginResult;
    }

    private Action.ActionBuilder goTo(LdapOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the AD Decision Node.
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
         * The AD user account password has expired.
         */
        PWD_EXPIRED,
        /**
         * The AD user account is disabled.
         */
        ACT_DISABLED,
        /**
         * The AD user account has expired.
         */
        ACT_EXPIRED,
        /**
         * The AD user account is locked.
         */
        ACT_LOCKED,
        /**
         * The user clicked the cancel button on the password change screen.
         */
        CANCELLED
    }

    /**
     * Defines the possible outcomes from the AD Decision Node.
     */
    public static class ADOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(ADDecisionNode.BUNDLE,
                    ADOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(LdapOutcome.TRUE.name(), bundle.getString("trueOutcome")),
                    new Outcome(LdapOutcome.FALSE.name(), bundle.getString("falseOutcome")),
                    new Outcome(LdapOutcome.PWD_EXPIRED.name(), bundle.getString("pwdExpiredOutcome")),
                    new Outcome(LdapOutcome.ACT_DISABLED.name(), bundle.getString("actDisabledOutcome")),
                    new Outcome(LdapOutcome.ACT_EXPIRED.name(), bundle.getString("actExpiredOutcome")),
                    new Outcome(LdapOutcome.ACT_LOCKED.name(), bundle.getString("actLockedOutcome")),
                    new Outcome(LdapOutcome.CANCELLED.name(), bundle.getString("cancelledOutcome")));
        }
    }

    @Override
    protected String getMtlsSecretIdTemplate() {
        return Labels.AD_DECISION_NODE_MTLS_CERT;
    }
}
