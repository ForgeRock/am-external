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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_TIMEOUT;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.HIDDEN_CALLCABK_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.POLICIES_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_CHECKSUM;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MIN_SHARED_SECRET_LENGTH;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TRUNCATION_OFFSET;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.HOTP_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.TOTP_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_CHALLENGE_KEY;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.oath.HashAlgorithm;
import org.forgerock.openam.auth.nodes.oath.NumberOfDigits;
import org.forgerock.openam.auth.nodes.oath.OathAlgorithm;
import org.forgerock.openam.auth.nodes.oath.OathRegistrationConfig;
import org.forgerock.openam.auth.nodes.oath.OathRegistrationHelper;
import org.forgerock.openam.auth.nodes.push.PushRegistrationConfig;
import org.forgerock.openam.auth.nodes.push.PushRegistrationHelper;
import org.forgerock.openam.auth.nodes.validators.JsonValidator;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.net.UrlEscapers;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * The Combined Multi-Factor Registration node is a registration node that does not authenticate a user but
 * allows a user already authenticated earlier to register their mobile device for both PUSH and OATH methods.
 */
@Node.Metadata(outcomeProvider = CombinedMultiFactorRegistrationNode.OutcomeProvider.class,
        configClass = CombinedMultiFactorRegistrationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class CombinedMultiFactorRegistrationNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(CombinedMultiFactorRegistrationNode.class);
    private static final String BUNDLE = CombinedMultiFactorRegistrationNode.class.getName();

    private static final String MFA_URI_SCHEME_QR_CODE_KEY = "mfauth";

    private final Config config;
    private final PushRegistrationHelper pushRegistrationHelper;
    private final OathRegistrationHelper oathRegistrationHelper;
    private final MultiFactorRegistrationUtilities multiFactorRegistrationUtilities;

    /**
     * The Combined MFA Registration node configuration.
     */
    public interface Config extends PushRegistrationConfig, OathRegistrationConfig {

        @Override
        @Attribute(order = 70)
        default int timeout() {
            return DEFAULT_TIMEOUT;
        }

        @Override
        @Attribute(order = 80)
        default NumberOfDigits passwordLength() {
            return NumberOfDigits.SIX_DIGITS;
        }

        @Override
        @Attribute(order = 90)
        default int minSharedSecretLength() {
            return DEFAULT_MIN_SHARED_SECRET_LENGTH;
        }

        @Override
        @Attribute(order = 100)
        default OathAlgorithm algorithm() {
            return OathAlgorithm.TOTP;
        }

        @Override
        @Attribute(order = 110)
        default int totpTimeInterval() {
            return DEFAULT_TOTP_INTERVAL;
        }

        @Override
        @Attribute(order = 120)
        default HashAlgorithm totpHashAlgorithm() {
            return HashAlgorithm.HMAC_SHA1;
        }

        @Override
        @Attribute(order = 130)
        default boolean addChecksum() {
            return DEFAULT_CHECKSUM;
        }

        @Override
        @Attribute(order = 140)
        default int truncationOffset() {
            return DEFAULT_TRUNCATION_OFFSET;
        }

        /**
         * Specifies the policies in JSON format.
         *
         * @return the JSON String containing the target policies.
         */
        @Attribute(order = 150, validators = {JsonValidator.class})
        default String policiesJson() {
            return "";
        }

        @Override
        default boolean postponeDeviceProfileStorage() {
            return false;
        }
    }

    /**
     * The MFA registration node constructor.
     *
     * @param config the node configuration.
     * @param mfaNodeDelegate shared utilities common to push implementations.
     * @param multiFactorRegistrationUtilities shared utilities for mfa registration operations.
     * @param pushRegistrationHelper the push registration helper class.
     * @param oathRegistrationHelper the oath registration helper class.
     * @param identityProvider the identity provider.
     */
    @Inject
    public CombinedMultiFactorRegistrationNode(@Assisted CombinedMultiFactorRegistrationNode.Config config,
                                               MultiFactorNodeDelegate<AuthenticatorPushService> mfaNodeDelegate,
                                               MultiFactorRegistrationUtilities multiFactorRegistrationUtilities,
                                               PushRegistrationHelper pushRegistrationHelper,
                                               OathRegistrationHelper oathRegistrationHelper,
                                               NodeUserIdentityProvider identityProvider) {
        super(mfaNodeDelegate, identityProvider);
        this.config = config;
        this.multiFactorRegistrationUtilities = multiFactorRegistrationUtilities;
        this.pushRegistrationHelper = pushRegistrationHelper;
        this.oathRegistrationHelper = oathRegistrationHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("MultiFactorRegistrationNode started.");

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
            if (!pushRegistrationHelper.isDeviceSettingsStored(username)
                    && !oathRegistrationHelper.isDeviceSettingsStored(username)) {
                logger.debug("User '{}' not registered for Push and OATH authentication. "
                        + "Starting registration process.", username);
                return startCombinedRegistration(context);
            } else {
                logger.debug("User '{}' already registered for Push and OATH authentication.", username);
                return buildAction(SUCCESS_OUTCOME_ID, context);
            }
        }
    }

    /**
     * Starts the registration for both Push and OATH.
     *
     * @param context the context of the tree authentication.
     * @return the next action to perform.
     * @throws NodeProcessException if unable to start registration process.
     */
    public Action startCombinedRegistration(TreeContext context) throws NodeProcessException {
        PushDeviceSettings pushDeviceSettings = pushRegistrationHelper.createDeviceSettings(config);
        OathDeviceSettings oathDeviceSettings = oathRegistrationHelper.createDeviceSettings(config);

        try {
            MessageId messageId = pushRegistrationHelper.createRegistrationMessage();
            String challenge = pushRegistrationHelper.createChallenge();
            AMIdentity userIdentity = getIdentity(context);

            List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings, oathDeviceSettings, userIdentity,
                    messageId.toString(), challenge, context);

            pushRegistrationHelper.updateMessageDispatcher(pushDeviceSettings, messageId, challenge);

            oathRegistrationHelper.updateSharedState(context.getStateFor(this), oathDeviceSettings,
                    config.generateRecoveryCodes());
            pushRegistrationHelper.updateSharedState(context.getStateFor(this), pushDeviceSettings,
                    messageId, challenge);

            return send(callbacks)
                    .replaceSharedState(context.sharedState.copy())
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    private Action getActionFromState(TreeContext context) throws NodeProcessException {
        try {
            PushDeviceSettings pushDeviceSettings = pushRegistrationHelper.getDeviceProfileFromSharedState(context);
            OathDeviceSettings oathDeviceSettings = oathRegistrationHelper.getDeviceProfileFromSharedState(context);
            if (pushDeviceSettings == null || oathDeviceSettings == null) {
                throw new NodeProcessException("No device profile found on shared state");
            }

            AMIdentity userIdentity = getIdentity(context);

            Action.ActionBuilder builder;
            MessageId messageId = pushRegistrationHelper.getMessageId(context);
            MessageState messageState = pushRegistrationHelper.getMessageState(messageId);
            if (messageState == null) {
                return pushRegistrationHelper.timeoutRegistration(context, messageId).build();
            }

            switch (messageState) {
            case SUCCESS:
                List<String> recoveryCodes = pushRegistrationHelper.handleSuccessMessage(messageId,
                        pushDeviceSettings, userIdentity, config.generateRecoveryCodes());
                oathRegistrationHelper.saveDeviceSettings(oathDeviceSettings, userIdentity, recoveryCodes);
                builder = pushRegistrationHelper.completeRegistration(context,
                        pushDeviceSettings, recoveryCodes);
                return cleanupSharedState(context, builder).build();

            case DENIED:
                builder = pushRegistrationHelper.failRegistration(context, messageId);
                return cleanupSharedState(context, builder).build();

            case UNKNOWN:
                String challenge = context.sharedState.get(PUSH_CHALLENGE_KEY).asString();
                List<Callback> callbacks = createScanQRCodeCallbacks(pushDeviceSettings, oathDeviceSettings,
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

    private List<Callback> createScanQRCodeCallbacks(PushDeviceSettings pushProfile, OathDeviceSettings oathProfile,
                                                     AMIdentity identity, String messageId, String challenge,
                                                     TreeContext context) throws NodeProcessException {

        Map<String, String> pushParams = pushRegistrationHelper.buildURIParameters(pushProfile, messageId,
                challenge, config, context);
        Map<String, String> oathParams = oathRegistrationHelper.buildURIParameters(oathProfile, config);

        Map<String, String> combinedParams = new LinkedHashMap<>();
        combinedParams.putAll(pushParams);
        combinedParams.putAll(oathParams);
        if (StringUtils.isNotEmpty(config.policiesJson())) {
            combinedParams.put(POLICIES_QR_CODE_KEY, Base64url.encode(config.policiesJson().getBytes()));
        }

        Callback textOutputCallback = pushRegistrationHelper.createLocalizedTextCallback(context, this.getClass(),
                config.scanQRCodeMessage());
        Callback qrCodeCallback = createQRCodeCallback(identity, combinedParams);
        Callback hiddenCallback = createHiddenCallback(identity, combinedParams);
        Callback pollingCallback = pushRegistrationHelper.createPollingWaitCallback();

        return ImmutableList.of(
                textOutputCallback,
                qrCodeCallback,
                hiddenCallback,
                pollingCallback
        );
    }

    private Callback createQRCodeCallback(AMIdentity identity, Map<String, String> params) {
        return multiFactorRegistrationUtilities.createQRCodeCallback(
                MFA_URI_SCHEME_QR_CODE_KEY,
                config.algorithm() == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(identity,
                        config.accountName().toString()),
                0,
                params
        );
    }

    private Callback createHiddenCallback(AMIdentity identity, Map<String, String> params) {
        return multiFactorRegistrationUtilities.createHiddenValueCallback(
                HIDDEN_CALLCABK_ID,
                MFA_URI_SCHEME_QR_CODE_KEY,
                config.algorithm() == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(identity,
                        config.accountName().toString()),
                params
        );
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        oathRegistrationHelper.clearSharedState(context.getStateFor(this));
        pushRegistrationHelper.clearSharedState(context.getStateFor(this));
        return builder.replaceSharedState(context.sharedState.copy());
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
