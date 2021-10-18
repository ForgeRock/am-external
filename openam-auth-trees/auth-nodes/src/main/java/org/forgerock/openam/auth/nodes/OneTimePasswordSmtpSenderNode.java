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
 * Copyright 2017-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.forgerock.openam.core.CoreWrapper;
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
        configClass = OneTimePasswordSmtpSenderNode.Config.class)
public class OneTimePasswordSmtpSenderNode extends OneTimePasswordNodeCommon {

    private static final String BUNDLE = OneTimePasswordSmtpSenderNode.class.getName()
            .replace('.', '/');
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final IdentityProvider identityProvider;

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
     *
     * @param config                the configuration for this Node.
     * @param coreWrapper           Instance of the CoreWrapper.
     * @param identityProvider      provides the current user's profile as an {@link AMIdentity}.
     * @param nodeSharedStateCrypto the crypto operations for encrypting/decrypting payloads
     */
    @Inject
    public OneTimePasswordSmtpSenderNode(@Assisted Config config,
                                         CoreWrapper coreWrapper,
                                         IdentityProvider identityProvider,
                                         NodeSharedStateCrypto nodeSharedStateCrypto) {
        super(nodeSharedStateCrypto);
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OneTimePasswordSmtpSenderNode started");

        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        if (!context.sharedState.isDefined(EMAIL_ADDRESS)) {
            String username = context.sharedState.get(USERNAME).asString();
            String realm = coreWrapper.convertRealmPathToRealmDn(context.sharedState.get(REALM).asString());
            AMIdentity identity = getAmIdentity(username, bundle, realm);

            context.sharedState.add(EMAIL_ADDRESS, getToEmailAddress(identity, username, bundle));
        }

        SMSGateway gateway = getSmsGateway(bundle);
        sendEmail(bundle, context.sharedState.get(EMAIL_ADDRESS).asString(), gateway, getClearTextOtp(context));

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

    private AMIdentity getAmIdentity(String username, ResourceBundle bundle, String realm) throws NodeProcessException {
        AMIdentity identity;
        try {
            identity = identityProvider.getIdentity(username, realm);
        } catch (IdRepoException | SSOException e) {
            logger.warn("Identity lookup failure", e);
            throw new NodeProcessException(bundle.getString("identity.failure"), e);
        }
        return identity;
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

}
