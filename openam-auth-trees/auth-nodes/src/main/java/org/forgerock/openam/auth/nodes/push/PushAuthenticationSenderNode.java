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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.LOADBALANCER_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MECHANISM_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.TIME_TO_LIVE_KEY;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CIBA_BINDING_MESSAGE;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.encode.Base64;

/**
 * Sends a push notification to the user when triggered, based on their username.
 *
 * @see org.forgerock.openam.core.rest.devices.push.PushDevicesResource
 * @see org.forgerock.openam.core.rest.devices.TwoFADevicesResource
 */
@Node.Metadata(outcomeProvider = PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider.class,
        configClass = PushAuthenticationSenderNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class PushAuthenticationSenderNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushAuthenticationSenderNode.class);
    private static final String BUNDLE = PushAuthenticationSenderNode.class.getName();

    private final Config config;
    private final UserPushDeviceProfileManager userPushDeviceProfileManager;
    private final PushNotificationService pushNotificationService;
    private final CoreWrapper coreWrapper;
    private final SessionCookies sessionCookies;
    private final SecondFactorNodeDelegate<AuthenticatorPushService> secondFactorNodeDelegate;
    private final LocaleSelector localeSelector;
    private final MessageIdFactory messageIdFactory;

    /**
     * Constructor.
     * @param config The node config.
     * @param userPushDeviceProfileManager Used to get the user push settings.
     * @param pushNotificationService Used to send push notifications.
     * @param coreWrapper Used to recover the users username.
     * @param sessionCookies Used to get the current servers lb cookie.
     * @param secondFactorNodeDelegate Shared utilities common to second factor implementations.
     * @param localeSelector Shared utilities to allow for overriding localisations.
     * @param messageIdFactory Used to create new MessageKeys for push messages.
     */
    @Inject
    public PushAuthenticationSenderNode(@Assisted PushAuthenticationSenderNode.Config config,
            UserPushDeviceProfileManager userPushDeviceProfileManager, PushNotificationService pushNotificationService,
            CoreWrapper coreWrapper, SessionCookies sessionCookies,
            SecondFactorNodeDelegate<AuthenticatorPushService> secondFactorNodeDelegate,
            LocaleSelector localeSelector, MessageIdFactory messageIdFactory) {
        this.config = config;
        this.userPushDeviceProfileManager = userPushDeviceProfileManager;
        this.pushNotificationService = pushNotificationService;
        this.coreWrapper = coreWrapper;
        this.sessionCookies = sessionCookies;
        this.secondFactorNodeDelegate = secondFactorNodeDelegate;
        this.localeSelector = localeSelector;
        this.messageIdFactory = messageIdFactory;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        String realm = context.sharedState.get(SharedStateConstants.REALM).asString();
        String username = context.sharedState.get(SharedStateConstants.USERNAME).asString();

        if (username == null) {
            LOGGER.debug("No username specified for push notification sender");
            throw new NodeProcessException("Expected username to be set.");
        }

        if (context.sharedState.get(PushNodeConstants.MESSAGE_ID_KEY).isNotNull()) {
            LOGGER.debug("Push notification sender has already been used (key already set in shared state)");
            throw new NodeProcessException("Message ID is already set! Must finish one push authentication before "
                    + "starting a new one.");
        }

        AMIdentity amIdentity = getIdentityFromIdentifier(realm, username);
        PushDeviceSettings device = getPushDeviceSettings(realm, amIdentity.getName());
        SkipSetting skippable = config.mandatory() ? SkipSetting.NOT_SKIPPABLE
                : secondFactorNodeDelegate.shouldSkip(amIdentity, realm);

        if (skippable == SkipSetting.SKIPPABLE) {
            LOGGER.debug("Push authentication sender node is being skipped");
            return Action.goTo(PushAuthenticationOutcomeProvider.PushAuthNOutcome.SKIPPED.name()).build();
        } else if (skippable == SkipSetting.NOT_SET || (skippable == SkipSetting.NOT_SKIPPABLE && device == null)) {
            //A state where a user has a device registered but whose SkipSetting is NOT_SET is not currently permissible
            LOGGER.debug("User has not registered a push device");
            return Action.goTo(PushAuthenticationOutcomeProvider.PushAuthNOutcome.NOT_REGISTERED.name()).build();
        }

        MessageId messageId = sendMessage(context, config, realm, username, device);

        return Action
                .goTo(PushAuthenticationOutcomeProvider.PushAuthNOutcome.SENT.name())
                .replaceSharedState(context.sharedState.copy().put(MESSAGE_ID_KEY, messageId.toString()))
                .build();
    }

    private MessageId sendMessage(TreeContext context, Config config, String realm, String username,
                               PushDeviceSettings device) throws NodeProcessException {

        String communicationId = device.getCommunicationId();
        String mechanismId = device.getDeviceMechanismUID();

        String challenge = userPushDeviceProfileManager.createRandomBytes();

        JwtClaimsSetBuilder jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim(MECHANISM_ID_KEY, mechanismId)
                .claim(LOADBALANCER_KEY, Base64.encode(sessionCookies.getLBCookie().getBytes()))
                .claim(CHALLENGE_KEY, challenge)
                .claim(TIME_TO_LIVE_KEY, String.valueOf(config.messageTimeout() / 1000));

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newHmacSigningHandler(Base64.decode(device.getSharedSecret())))
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.HS256).done().build();

        String pushMessage = getLocalisedUserMessage(context, config);

        pushMessage = pushMessage.replaceAll("\\{\\{user\\}\\}", username);
        pushMessage = pushMessage.replaceAll("\\{\\{issuer\\}\\}", device.getIssuer());

        MessageId messageId = messageIdFactory.create(DefaultMessageTypes.AUTHENTICATE);
        PushMessage message = new PushMessage(communicationId, jwt, pushMessage, messageId);

        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(
                new SignedJwtVerificationPredicate(Base64.decode(device.getSharedSecret()), JWT));
        servicePredicates.add(
                new PushMessageChallengeResponsePredicate(Base64.decode(device.getSharedSecret()), challenge, JWT));

        PushNotificationService pushNotificationService = getPushNotificationService(realm);

        try {
            Set<Predicate> predicates = pushNotificationService.getMessagePredicatesFor(realm)
                    .get(DefaultMessageTypes.AUTHENTICATE);
            if (predicates != null) {
                servicePredicates.addAll(predicates);
            }

            pushNotificationService.getMessageDispatcher(realm).expectInCluster(messageId, servicePredicates);
            pushNotificationService.send(message, realm);
            LOGGER.debug("Push authentication has been sent");
            return message.getMessageId();
        } catch (NotFoundException | PushNotificationException e) {
            LOGGER.error("AuthenticatorPush :: sendMessage() : Failed to transmit message through PushService.");
            throw new NodeProcessException(e);
        } catch (CoreTokenException e) {
            LOGGER.error("Unable to persist message token in core token service.", e);
            throw new NodeProcessException(e);
        }
    }

    // The username in the shared state is not necessarily the actual username of the user (example: email address)
    private AMIdentity getIdentityFromIdentifier(String realm, String identifier) throws NodeProcessException {
        AMIdentity identity = coreWrapper.getIdentity(identifier, realm);
        if (identity == null) {
            throw new NodeProcessException("Failed to fetch identity.");
        }
        return identity;
    }

    private PushNotificationService getPushNotificationService(String realm) throws NodeProcessException {
        try {
            pushNotificationService.init(realm);
            return pushNotificationService;
        } catch (PushNotificationException e) {
            throw new NodeProcessException(e);
        }
    }

    private PushDeviceSettings getPushDeviceSettings(String realm, String username) throws NodeProcessException {
        try {
            return CollectionUtils.getFirstItem(userPushDeviceProfileManager.getDeviceProfiles(username, realm));
        } catch (DevicePersistenceException dpe) {
            throw new NodeProcessException(dpe);
        }
    }

    /**
     * Checks for the presence of the binding_message property that comes with a Client Initiated Backchannel
     * Authentication request, returning the value if present.
     *
     * @param context the current {@link TreeContext} which will provide access to request attributes.
     * @return an {@link Optional} containing either the binding message or nothing.
     */
    private Optional<String> checkForBindingMessage(TreeContext context) {
        String bindingMessage = (String) context.request.servletRequest.getAttribute(CIBA_BINDING_MESSAGE);
        return Optional.ofNullable(bindingMessage);
    }

    private String getLocalisedUserMessage(TreeContext context, Config config) {
        return checkForBindingMessage(context).orElseGet(() -> {
            PreferredLocales preferredLocales = context.request.locales;
            Locale bestLocale = localeSelector.getBestLocale(preferredLocales, config.userMessage().keySet());

            if (bestLocale != null) {
                return config.userMessage().get(bestLocale);
            }

            ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(PushAuthenticationSenderNode.BUNDLE,
                    PushAuthenticationSenderNode.class.getClassLoader());
            return bundle.getString("default.user.message");
        });
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * An ordered list of choices to be displayed to the user.
         * @return The choices.
         */
        @Attribute(order = 100)
        default int messageTimeout() {
            return 120000;
        }

        /**
         * Allows administrators to override the properties files.
         * @return The mapping of locales to user messages.
         */
        @Attribute(order = 200)
        default Map<Locale, String> userMessage() {
            return Collections.emptyMap();
        }

        /**
         * Allows administrators to remove the 'skipped' outcome.
         * @return Enforces this node in the tree.
         */
        @Attribute(order = 300)
        default boolean mandatory() {
            return false;
        }
    }

    /**
     * Provides the outcomes for the push authentication sender node.
     */
    public static class PushAuthenticationOutcomeProvider implements OutcomeProvider {
        /**
         * The possible outcomes for the PushAuthenticationSenderNode.
         */
        public enum PushAuthNOutcome {
            /**
             * Push Message successfully sent.
             */
            SENT("Sent"),
            /**
             * User does not have a registered device.
             */
            NOT_REGISTERED("Not Registered"),
            /**
             * The user has configured 2FA to be skipped, and this has not been overridden by node config.
             */
            SKIPPED("Skipped");

            String displayValue;

            /**
             * Constructor.
             * @param displayValue The value which is displayed to the user.
             */
            PushAuthNOutcome(String displayValue) {
                this.displayValue = displayValue;
            }

            private Outcome getOutcome() {
                return new Outcome(name(), displayValue);
            }
        }

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

            List<Outcome> results = new ArrayList<>(
                    Arrays.asList(PushAuthNOutcome.SENT.getOutcome(),
                    PushAuthNOutcome.NOT_REGISTERED.getOutcome()));

            if (nodeAttributes.isNotNull()) {
                // nodeAttributes is null when the node is created
                if (!nodeAttributes.get("mandatory").required().asBoolean()) {
                    results.add(PushAuthNOutcome.SKIPPED.getOutcome());
                }
            } else {
                results.add(PushAuthNOutcome.SKIPPED.getOutcome());
            }

            return Collections.unmodifiableList(results);
        }
    }
}
