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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.modules.hotp.SMSGatewayLookup;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * A node that can send a message via SMTP using pre-configured parameters.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = OneTimePasswordSmtpSenderNode.Config.class,
        tags = {"otp", "mfa", "multi-factor authentication"})
public class OneTimePasswordSmtpSenderNode extends SingleOutcomeNode {
    private final OtpNodeConnectionConfigMapper connectionConfigMapper;

    private static final String BUNDLE = OneTimePasswordSmtpSenderNode.class.getName();
    private final Logger logger = LoggerFactory.getLogger(OneTimePasswordSmtpSenderNode.class);
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;
    private final LocaleSelector localeSelector;
    private final SMSGatewayLookup smsGatewayLookup;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;

    /**
     * Configuration for the one time password SMTP sender node.
     */
    public interface Config extends OtpNodeBaseConfig {
        /**
         * The key used to look up the email address in an identity.
         *
         * @return email address attribute.
         */
        @Attribute(order = 1100)
        default String emailAttribute() {
            return "mail";
        }

        /**
         * The subject of the email.
         * @return the email title
         */
        @Attribute(order = 1300)
        Map<Locale, String> emailSubject();

        /**
         * The content of the email.
         *
         * <p>{@literal {{OTP}}} will be replaced with the value of the OTP.</p>
         * @return the email content
         */
        @Attribute(order = 1400)
        Map<Locale, String> emailContent();

        /**
         * The (optional) password purpose to use when the mail server is using SMTP authentication.
         * @return The password purpose.
         */
        @Override
        @Attribute(order = 550, resourceName = "secretLabelIdentifier")
        @SecretPurpose("am.authentication.nodes.otp.mail.%s.password")
        Optional<Purpose<GenericSecret>> passwordPurpose();
    }

    /**
     * Creates an EmailNode with the provided Config.
     *
     * @param connectionConfigMapper  the config mapper.
     * @param config                  the configuration for this Node.
     * @param realm                   The current realm.
     * @param identityProvider        The NodeUserIdentityProvider.
     * @param idmIntegrationService   Allows collaboration with platform-enabled nodes.
     * @param localeSelector          An instance of LocaleSelector.
     * @param smsGatewayLookup        lookup for {@link SMSGateway}.
     */
    @Inject
    public OneTimePasswordSmtpSenderNode(OtpNodeConnectionConfigMapper connectionConfigMapper, @Assisted Config config,
            @Assisted Realm realm, NodeUserIdentityProvider identityProvider,
            IdmIntegrationService idmIntegrationService, LocaleSelector localeSelector,
            SMSGatewayLookup smsGatewayLookup) {
        this.connectionConfigMapper = connectionConfigMapper;
        this.config = config;
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
        this.localeSelector = localeSelector;
        this.smsGatewayLookup = smsGatewayLookup;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OneTimePasswordSmtpSenderNode started");

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        NodeState nodeState = context.getStateFor(this);
        String email = getEmailFromContext(context);
        if (email == null) {
            String username = context.sharedState.get(USERNAME).asString();
            Optional<AMIdentity> identity = identityProvider.getAMIdentity(context.universalId, nodeState);
            if (identity.isEmpty()) {
                logger.warn("Identity lookup failed");
                throw new NodeProcessException(bundle.getString("identity.failure"));
            }
            email = getToEmailAddress(identity.get(), username, bundle);
            context.sharedState.add(EMAIL_ADDRESS, email);
        }

        SMSGateway gateway = getSmsGateway(bundle);

        JsonValue oneTimePassword = context.getState(ONE_TIME_PASSWORD);
        if (oneTimePassword == null) {
            logger.warn("oneTimePasswordNotFound");
            throw new NodeProcessException(bundle.getString("oneTimePassword.not.found"));
        }
        sendEmail(context, bundle, email, gateway, oneTimePassword.asString());

        return goToNext().replaceSharedState(context.sharedState.copy()).build();
    }

    private void sendEmail(TreeContext context, ResourceBundle bundle, String toEmailAddress, SMSGateway gateway,
            String oneTimePassword) throws NodeProcessException {
        try {
            String subject = getLocalisedMessage(context, localeSelector, this.getClass(), config.emailSubject(),
                    "messageSubject");
            String content = getLocalisedMessage(context, localeSelector, this.getClass(), config.emailContent(),
                    "messageContent");
            logger.debug("sending one time password from {}, to {}", config.fromEmailAddress(), toEmailAddress);
            gateway.sendEmail(config.fromEmailAddress(), toEmailAddress, subject,
                    content, oneTimePassword, connectionConfigMapper.asConfigMap(config, realm));
        } catch (AuthLoginException e) {
            logger.warn("Email sending failure", e);
            throw new NodeProcessException(bundle.getString("send.failure"), e);
        }
    }

    private SMSGateway getSmsGateway(ResourceBundle bundle) throws NodeProcessException {
        try {
            return smsGatewayLookup.getSmsGateway(config.smsGatewayImplementationClass());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.warn("SMSGateway error", e);
            throw new NodeProcessException(bundle.getString("gateway.failure"), e);
        }
    }

    private String getToEmailAddress(AMIdentity identity, String username,
            ResourceBundle bundle) throws NodeProcessException {
        String toEmailAddress;
        try {
            toEmailAddress = getEmailAddress(identity, username);
            if (toEmailAddress == null) {
                logger.warn("Email not found");
                throw new NodeProcessException(bundle.getString("email.not.found"));
            }
        } catch (IdRepoException | SSOException e) {
            logger.warn("Email lookup failure", e);
            throw new NodeProcessException(bundle.getString("email.lookup.failure"), e);
        }
        return toEmailAddress;
    }

    private String getEmailFromContext(TreeContext context) {
        if (context.sharedState.isDefined(EMAIL_ADDRESS)) {
            return context.sharedState.get(EMAIL_ADDRESS).asString();
        } else {
            return getAttributeFromContext(idmIntegrationService, context, config.emailAttribute())
                    .map(JsonValue::asString)
                    .orElse(null);
        }
    }

    /**
     * Gets the Email address of the user.
     *
     * @param identity The user's identity.
     * @param userName the username used to look up the identity
     * @return The user's email address.
     * @throws IdRepoException If there is a problem getting the user's email address.
     * @throws SSOException    If there is a problem getting the user's email address.
     */
    private String getEmailAddress(AMIdentity identity, String userName) throws IdRepoException, SSOException {
        String emailAttribute = config.emailAttribute();
        if (StringUtils.isBlank(emailAttribute)) {
            emailAttribute = "mail";
        }

        logger.debug("Using email attribute of {}", emailAttribute);

        Set<String> emails = identity.getAttribute(emailAttribute);
        String mail = null;

        if (CollectionUtils.isNotEmpty(emails)) {
            mail = emails.iterator().next();
            logger.debug("Email address found {} with username : {}", mail, userName);
        } else {
            logger.debug("no email found with username : {}", userName);
        }

        return mail;
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(ONE_TIME_PASSWORD),
            new InputState(EMAIL_ADDRESS, false),
            new InputState(USERNAME, false),
            new InputState(REALM, false)
        };
    }
}
