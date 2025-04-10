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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.MFA_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.OATH_METHOD;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_ALLOW_RECOVERY_CODES;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_HOTP_WINDOW_SIZE;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MAXIMUM_ALLOWED_CLOCK_DRIFT;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_TIME_STEPS;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.auth.nodes.validators.GreaterThanZeroValidator;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The OATH Verifier node collects and verify the One-Time Password code from the user, which is generated by their
 * earlier registered mobile device.
 */
@Node.Metadata(outcomeProvider = OathTokenVerifierNode.OutcomeProvider.class,
        configClass = OathTokenVerifierNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class OathTokenVerifierNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(OathTokenVerifierNode.class);
    private static final String BUNDLE = OathTokenVerifierNode.class.getName();

    static final int SUBMIT = 0;
    static final int RECOVERY = 1;

    /**
     * Key to use in the session's AuthType. Intentionally matches the type used for the OATH Auth Module, to allow the
     * use of device management from the Dashboard.
     */
    static final String OATH_AUTH_TYPE = "AuthenticatorOATH";

    private final OathTokenVerifierNode.Config config;
    private final OathDeviceProfileHelper deviceProfileHelper;

    /**
     * The constructor.
     *
     * @param config the node configuration.
     * @param deviceProfileHelper stores device profiles.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityProvider the identity provider.
     */
    @Inject
    public OathTokenVerifierNode(@Assisted Config config,
            OathDeviceProfileHelper deviceProfileHelper,
            MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate,
            NodeUserIdentityProvider identityProvider) {
        super(multiFactorNodeDelegate, identityProvider);
        this.config = config;
        this.deviceProfileHelper = deviceProfileHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OathTokenVerifierNode started.");

        String realm = context.sharedState.get(SharedStateConstants.REALM).asString();
        String username = context.sharedState.get(SharedStateConstants.USERNAME).asString();

        if (username == null) {
            throw new NodeProcessException("Expected username to be set.");
        }

        // If postponing the device storage, retrieve device profile from sharedState
        OathDeviceSettings oathDeviceSettings = context.getStateFor(this).isDefined(OATH_DEVICE_PROFILE_KEY)
                ? deviceProfileHelper.getDeviceProfileFromSharedState(context, OATH_DEVICE_PROFILE_KEY)
                : deviceProfileHelper.getDeviceSettings(realm, username);
        if (oathDeviceSettings == null) {
            logger.debug("User '{}' not registered for OATH.", username);
            return Action.goTo(NOT_REGISTERED_OUTCOME_ID)
                    .replaceSharedState(context.sharedState.copy().put(MFA_METHOD, OATH_METHOD))
                    .build();
        }

        Optional<ConfirmationCallback> confirmationCallback = context.getCallback(ConfirmationCallback.class);
        if (confirmationCallback.isPresent() && confirmationCallback.get().getSelectedIndex() == RECOVERY) {
            logger.debug("Recovery Code button pressed, returning with recovery code outcome.");
            return buildAction(RECOVERY_CODE_OUTCOME_ID, context);
        }

        Optional<NameCallback> nameCallback = context.getCallback(NameCallback.class);
        if (!nameCallback.isPresent()) {
            logger.debug("Ask for the OTP code.");
            return Action.send(getCallbacks(context)).build();
        }

        try {
            verifyCode(String.valueOf(nameCallback.get().getName()), oathDeviceSettings);
            logger.debug("OTP code verified, saving device settings.");
            oathDeviceSettings.setLastAccessDate(Time.currentTimeMillis());
            deviceProfileHelper.saveDeviceSettings(realm, username, oathDeviceSettings);
            return Action.goTo(SUCCESS_OUTCOME_ID)
                    .addNodeType(context, OATH_AUTH_TYPE)
                    .build();
        } catch (OathVerificationException e) {
            logger.error(e.getMessage(), e);
            return buildAction(FAILURE_OUTCOME_ID, context);
        }
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        return builder;
    }

    private List<Callback> getCallbacks(TreeContext context) {
        List<String> options = new ArrayList<>();
        options.add(getString(context, "default.submit"));
        if (config.isRecoveryCodeAllowed()) {
            options.add(getString(context, "default.recoveryCodes"));
        }
        return ImmutableList.of(
                new NameCallback(getString(context, "default.message")),
                new ConfirmationCallback(
                        ConfirmationCallback.INFORMATION,
                        options.toArray(new String[0]),
                        SUBMIT)
        );
    }

    private String getString(TreeContext context, String key) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        return bundle.getString(key);
    }

    /**
     * Verifies the input OTP.
     *
     * @param otp the OTP to verify.
     * @param settings with which the OTP was configured.
     * @throws OathVerificationException on any error validating the token code.
     */
    private void verifyCode(String otp, OathDeviceSettings settings) throws OathVerificationException {
        // check password length, MUST be 6 or 8
        if ((otp.length() != 6) && (otp.length() != 8)) {
            throw new OathVerificationException("Password length should be 6 or 8.");
        }

        AbstractOathVerifier verifier;
        if (OathAlgorithm.HOTP.equals(config.algorithm())) {
            verifier = new OathHotpVerifier(config, settings);
        } else if (OathAlgorithm.TOTP.equals(config.algorithm())) {
            verifier = new OathTotpVerifier(config, settings);
        } else {
            throw new OathVerificationException("Invalid OTP algorithm");
        }

        verifier.verify(otp);
    }

    /**
     * The OATH Verifier node configuration.
     */
    public interface Config {

        /**
         * Specifies the OATH Algorithm to Use.
         *
         * @return the the algorithm the device uses to generate the OTP.
         */
        @Attribute(order = 10)
        default OathAlgorithm algorithm() {
            return OathAlgorithm.TOTP;
        }

        /**
         * Specifies the HOTP Window Size.
         *
         * @return the size of the window to resynchronize with the client.
         */
        @Attribute(order = 20, validators = {RequiredValueValidator.class, GreaterThanZeroValidator.class})
        default int hotpWindowSize() {
            return DEFAULT_HOTP_WINDOW_SIZE;
        }

        /**
         * Specifies the TOTP time step interval.
         *
         * @return the TOTP time step in seconds that the OTP device uses to generate the OTP.
         */
        @Attribute(order = 30, validators = {RequiredValueValidator.class, GreaterThanZeroValidator.class})
        default int totpTimeInterval() {
            return DEFAULT_TOTP_INTERVAL;
        }

        /**
         * Specifies the TOTP Time Steps.
         *
         * @return the number of time steps to check before and after receiving a OTP.
         */
        @Attribute(order = 40, validators = {RequiredValueValidator.class, GreaterThanZeroValidator.class})
        default int totpTimeSteps() {
            return DEFAULT_TOTP_TIME_STEPS;
        }

        /**
         * Specifies the TOTP hash algorithm.
         *
         * @return the TOTP hash algorithm to be used to generate the OTP.
         */
        @Attribute(order = 50)
        default HashAlgorithm totpHashAlgorithm() {
            return HashAlgorithm.HMAC_SHA1;
        }

        /**
         * Specifies the Maximum Allowed Clock Drift.
         *
         * @return the number of time steps a client is allowed to get out of sync.
         */
        @Attribute(order = 60, validators = {RequiredValueValidator.class, GreaterThanZeroValidator.class})
        default int maximumAllowedClockDrift() {
            return DEFAULT_MAXIMUM_ALLOWED_CLOCK_DRIFT;
        }

        /**
         * If true, allows rendering of a button to use a recovery code.
         *
         * @return is the recovery code button shown.
         */
        @Attribute(order = 70)
        default boolean isRecoveryCodeAllowed() {
            return DEFAULT_ALLOW_RECOVERY_CODES;
        }
    }

    /**
     * Provides the oath verifier node's set of outcomes.
     */
    public static final class OutcomeProvider implements BoundedOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return getAllOutcomes(locales).stream()
                    .filter(outcome -> {
                        if (RECOVERY_CODE_OUTCOME_ID.equals(outcome.id)) {
                            return nodeAttributes.isNotNull()
                                           && nodeAttributes.get("isRecoveryCodeAllowed").required().asBoolean();
                        }
                        return true;
                    }).toList();
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    OathTokenVerifierNode.OutcomeProvider.class.getClassLoader());
            return List.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)),
                    new Outcome(NOT_REGISTERED_OUTCOME_ID, bundle.getString(NOT_REGISTERED_OUTCOME_ID)),
                    new Outcome(RECOVERY_CODE_OUTCOME_ID, bundle.getString(RECOVERY_CODE_OUTCOME_ID)
            ));
        }
    }
}
