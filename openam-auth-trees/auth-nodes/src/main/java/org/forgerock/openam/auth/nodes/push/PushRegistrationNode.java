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
 * Copyright 2020-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CHALLENGE_KEY;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Authenticator Push Registration node is a registration node that does not authenticate a user but
 * allows a user already authenticated earlier to register their mobile device.
 */
@Node.Metadata(outcomeProvider = PushRegistrationNode.OutcomeProvider.class,
        configClass = PushRegistrationConfig.class,
        tags = {"mfa", "multi-factor authentication"})
public class PushRegistrationNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationNode.class);
    private static final String BUNDLE = PushRegistrationNode.class.getName();

    private final PushRegistrationConfig config;
    private final PushRegistrationHelper pushRegistrationHelper;

    /**
     * The Push registration node constructor.
     *
     * @param config                  the node configuration.
     * @param realm                   the realm.
     * @param coreWrapper             the {@code CoreWrapper} instance.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityService         an instance of the IdentityService.
     * @param pushRegistrationHelper  the push registration helper class.
     */
    @Inject
    public PushRegistrationNode(@Assisted PushRegistrationConfig config,
                                @Assisted Realm realm,
                                CoreWrapper coreWrapper,
                                MultiFactorNodeDelegate<AuthenticatorPushService> multiFactorNodeDelegate,
                                LegacyIdentityService identityService,
                                PushRegistrationHelper pushRegistrationHelper) {
        super(realm, coreWrapper, multiFactorNodeDelegate, identityService);
        this.config = config;
        this.pushRegistrationHelper = pushRegistrationHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("PushRegistrationNode started.");

        Optional<PollingWaitCallback> pollingWaitCallback = context.getCallback(PollingWaitCallback.class);
        if (pollingWaitCallback.isPresent()) {
            logger.debug("Registration process already started. Waiting for completion...");
            if (!context.getStateFor(this).isDefined(MESSAGE_ID_KEY)) {
                logger.error("Unable to find push message ID in sharedState.");
                throw new NodeProcessException("Unable to find push message ID");
            }
            return getActionFromState(context);
        } else {
            String username = context.getStateFor(this).isDefined(USERNAME)
                    ? context.getStateFor(this).get(USERNAME).asString()
                    : null;
            if (StringUtils.isBlank(username)) {
                logger.error("No username found.");
                throw new NodeProcessException("Expected username to be set.");
            }
            if (!pushRegistrationHelper.isDeviceSettingsStored(username)) {
                logger.debug("User '{}' not registered for Push Authentication. "
                        + "Starting registration process.", username);
                return startRegistration(context);
            } else {
                logger.debug("User '{}' already registered for Push Authentication.", username);
                return buildAction(SUCCESS_OUTCOME_ID, context);
            }
        }
    }

    private Action getActionFromState(TreeContext context) throws NodeProcessException {
        try {
            PushDeviceSettings pushDeviceSettings = pushRegistrationHelper.getDeviceProfileFromSharedState(context);
            if (pushDeviceSettings == null) {
                throw new NodeProcessException("No device profile found on shared state");
            }

            AMIdentity userIdentity = getIdentity(context);

            MessageId messageId = pushRegistrationHelper.getMessageId(context);
            MessageState messageState = pushRegistrationHelper.getMessageState(messageId);
            if (messageState == null) {
                return pushRegistrationHelper.timeoutRegistration(context, messageId).build();
            }

            switch (messageState) {
            case SUCCESS:
                List<String> recoveryCodes = pushRegistrationHelper.handleSuccessMessage(messageId,
                        pushDeviceSettings, userIdentity, config.generateRecoveryCodes());
                return pushRegistrationHelper.completeRegistration(context, pushDeviceSettings,
                        recoveryCodes).build();

            case DENIED:
                return pushRegistrationHelper.failRegistration(context, messageId).build();

            case UNKNOWN:
                String challenge = context.sharedState.get(PUSH_CHALLENGE_KEY).asString();
                List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings,
                        userIdentity, messageId.toString(), challenge, context);
                return pushRegistrationHelper.waitRegistration(context, callbacks, config.timeout()).build();

            default:
                throw new NodeProcessException("Unrecognized push message status: " + messageState);
            }
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Unable initialise push notification service.", e);
        } catch (CoreTokenException e) {
            throw new NodeProcessException("An unexpected error occurred while verifying the push result", e);
        }
    }

    private Action startRegistration(TreeContext context) throws NodeProcessException {
        PushDeviceSettings pushDeviceSettings = pushRegistrationHelper.createDeviceSettings(config);
        try {
            MessageId messageId = pushRegistrationHelper.createRegistrationMessage();
            String challenge = pushRegistrationHelper.createChallenge();
            AMIdentity userIdentity = getIdentity(context);

            List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings, userIdentity,
                    messageId.toString(), challenge, context);

            pushRegistrationHelper.updateMessageDispatcher(pushDeviceSettings, messageId, challenge);

            pushRegistrationHelper.updateSharedState(context.getStateFor(this), pushDeviceSettings,
                    messageId, challenge);

            return send(callbacks)
                    .replaceSharedState(context.sharedState.copy())
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    private List<Callback> createScanQRCodeCallbacks(PushDeviceSettings deviceProfile,
                                                    AMIdentity identity, String messageId, String challenge,
                                                    TreeContext context) throws NodeProcessException {
        Map<String, String> params = pushRegistrationHelper.buildURIParameters(deviceProfile, messageId,
                challenge, this.config, context);

        Callback textOutputCallback = pushRegistrationHelper.createLocalizedTextCallback(context, this.getClass(),
                config.scanQRCodeMessage());
        Callback hiddenCallback = pushRegistrationHelper.createHiddenCallback(identity, params, config.issuer(),
                config.accountName().toString());
        Callback qrCodeCallback = pushRegistrationHelper.createQRCodeCallback(identity, params, config.issuer(),
                config.accountName().toString());
        Callback pollingCallback = pushRegistrationHelper.createPollingWaitCallback();

        return ImmutableList.of(
                textOutputCallback,
                qrCodeCallback,
                hiddenCallback,
                pollingCallback
        );
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        return pushRegistrationHelper.cleanupSharedState(context, builder);
    }

    /**
     * Provides the push registration node's set of outcomes.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)),
                    new Outcome(TIMEOUT_OUTCOME_ID, bundle.getString(TIMEOUT_OUTCOME_ID)));
        }
    }
}
