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
 * Copyright 2017-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getAttributeFromContext;
import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
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

    private static final String BUNDLE = OneTimePasswordSmtpSenderNode.class.getName();
    private final Logger logger = LoggerFactory.getLogger(OneTimePasswordSmtpSenderNode.class);
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final IdentityUtils identityUtils;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration for the one time password SMTP sender node.
     */
    public interface Config extends SmtpBaseConfig {
        /**
         * The key used to look up the email address in an identity.
         *
         * @return email address attribute.
         */
        @Attribute(order = 1100)
        default String emailAttribute() {
            return "mail";
        }
    }

    /**
     * Creates an EmailNode with the provided Config.
     * @param config the configuration for this Node.
     * @param coreWrapper Instance of the CoreWrapper.
     * @param identityUtils An instance of the IdentityUtils.
     * @param idmIntegrationService Allows collaboration with platform-enabled nodes.
     */
    @Inject
    public OneTimePasswordSmtpSenderNode(@Assisted Config config, CoreWrapper coreWrapper,
            IdentityUtils identityUtils, IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.identityUtils = identityUtils;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OneTimePasswordSmtpSenderNode started");

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        String email = getEmailFromContext(context);
        if (email == null) {
            String username = context.sharedState.get(USERNAME).asString();
            Optional<AMIdentity> identity = getAMIdentity(context, identityUtils, coreWrapper);
            if (identity.isEmpty()) {
                logger.warn("Identity lookup failed");
                throw new NodeProcessException(bundle.getString("identity.failure"));
            }
            email = getToEmailAddress(identity.get(), username, bundle);
            context.sharedState.add(EMAIL_ADDRESS, email);
        }

        SMSGateway gateway = getSmsGateway(bundle);
        String oneTimePassword = context.sharedState.get(ONE_TIME_PASSWORD).asString();
        sendEmail(bundle, email, gateway, oneTimePassword);

        return goToNext().replaceSharedState(context.sharedState.copy()).build();
    }

    private void sendEmail(ResourceBundle bundle, String toEmailAddress, SMSGateway gateway,
            String oneTimePassword) throws NodeProcessException {
        try {
            logger.debug("sending one time password from {}, to {}", config.fromEmailAddress(), toEmailAddress);
            gateway.sendEmail(config.fromEmailAddress(), toEmailAddress, bundle.getString("messageSubject"),
                    bundle.getString("messageContent"), oneTimePassword, config.asConfigMap());
        } catch (AuthLoginException e) {
            logger.warn("Email sending failure", e);
            throw new NodeProcessException(bundle.getString("send.failure"), e);
        }
    }

    private SMSGateway getSmsGateway(ResourceBundle bundle) throws NodeProcessException {
        SMSGateway gateway;
        try {
            gateway = Class.forName(config.smsGatewayImplementationClass()).asSubclass(SMSGateway.class).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.warn("SMSGateway error", e);
            throw new NodeProcessException(bundle.getString("gateway.failure"), e);
        }
        return gateway;
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
