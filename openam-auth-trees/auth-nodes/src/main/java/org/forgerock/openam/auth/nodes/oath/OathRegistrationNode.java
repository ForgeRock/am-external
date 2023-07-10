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

package org.forgerock.openam.auth.nodes.oath;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.BGCOLOUR_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_BG_COLOR;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_GENERATE_RECOVERY_CODES;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_ISSUER;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.HIDDEN_CALLCABK_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.IMG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.ISSUER_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.SCAN_QR_CODE_MSG_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.ALGORITHM_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.COUNTER_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_CHECKSUM;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_MIN_SHARED_SECRET_LENGTH;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TOTP_INTERVAL;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DEFAULT_TRUNCATION_OFFSET;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DIGITS_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.HOTP_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_URI_SCHEME_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.PERIOD_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.SECRET_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.TOTP_URI_HOST_QR_CODE_KEY;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.LocalizationHelper;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.net.UrlEscapers;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The OATH Registration node is a registration node that does not authenticate a user but
 * allows a user already authenticated earlier to register their mobile device.
 */
@Node.Metadata(outcomeProvider = OathRegistrationNode.OutcomeProvider.class,
        configClass = OathRegistrationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class OathRegistrationNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(OathRegistrationNode.class);
    private static final String BUNDLE = OathRegistrationNode.class.getName();

    private static final int NEXT_OPTION = 0;
    @VisibleForTesting
    static final String NEXT_LABEL = "Next";
    private static final String NEXT_LABEL_KEY_NAME = "nextButtonLabel";

    private final Config config;
    private final Realm realm;
    private final OathDeviceProfileHelper deviceProfileHelper;
    private final LocalizationHelper localizationHelper;

    /**
     * The Oath registration node constructor.
     *
     * @param config the node configuration.
     * @param realm the realm.
     * @param coreWrapper the {@code CoreWrapper} instance.
     * @param deviceProfileHelper stores device profiles.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityUtils an instance of the IdentityUtils.
     * @param localizationHelper the localization helper class.
     */
    @Inject
    public OathRegistrationNode(@Assisted Config config,
            @Assisted Realm realm,
            CoreWrapper coreWrapper,
            OathDeviceProfileHelper deviceProfileHelper,
            MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate,
            IdentityUtils identityUtils,
            LocalizationHelper localizationHelper) {
        super(realm, coreWrapper, multiFactorNodeDelegate, identityUtils);
        this.config = config;
        this.realm = realm;
        this.deviceProfileHelper = deviceProfileHelper;
        this.localizationHelper = localizationHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OathRegistrationNode started.");

        String username = context.sharedState.get(USERNAME).asString();
        if (StringUtils.isBlank(username)) {
            logger.error("No username found.");
            throw new NodeProcessException("Expected username to be set.");
        }

        if (context.getCallback(ConfirmationCallback.class)
                .filter(callback -> callback.getSelectedIndex() == NEXT_OPTION)
                .isPresent()) {

            AMIdentity userIdentity = getIdentity(context);

            OathDeviceSettings oathDeviceSettings = deviceProfileHelper
                    .getDeviceProfileFromSharedState(context, OATH_DEVICE_PROFILE_KEY);
            if (oathDeviceSettings == null) {
                logger.error("No device profile found on shared state");
                return buildAction(FAILURE_OUTCOME_ID, context);
            }

            logger.debug("Saving oath device profile.");
            List<String> recoveryCodes = deviceProfileHelper.saveDeviceSettings(oathDeviceSettings,
                    json(object()), userIdentity, config.generateRecoveryCodes());
            setUserToNotSkippable(userIdentity, realm.toString());

            if (CollectionUtils.isNotEmpty(recoveryCodes)) {
                logger.debug("Completed OATH registration. Sending recovery codes.");
                return buildActionWithRecoveryCodes(context, oathDeviceSettings.getDeviceName(), recoveryCodes);
            } else {
                logger.debug("Completed OATH registration.");
                return buildAction(SUCCESS_OUTCOME_ID, context);
            }
        }

        return startRegistration(context);
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        return builder;
    }

    /**
     * Starts the Oath Registration process.
     *
     * @param context The context of the tree authentication.
     * @return The next action to perform.
     */
    private Action startRegistration(TreeContext context) throws NodeProcessException {
        OathDeviceSettings oathDeviceSettings = deviceProfileHelper.createDeviceSettings(
                config.minSharedSecretLength(),
                config.addChecksum(),
                config.truncationOffset()
        );
        try {
            AMIdentity userIdentity = getIdentity(context);

            List<Callback> callbacks = createScanQRCodeCallbacks(context, oathDeviceSettings, userIdentity);

            JsonValue sharedState = context.sharedState.copy()
                    .put(OATH_DEVICE_PROFILE_KEY, deviceProfileHelper.encodeDeviceSettings(oathDeviceSettings));

            return send(callbacks)
                    .replaceSharedState(sharedState)
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Creates a set of callbacks used to display the QRCode for scanning.
     *
     * @param context The context of the tree authentication.
     * @param deviceProfile the Oath device's settings.
     * @param identity the AM identity.
     * @return List of callbacks used for scan the QRCode.
     * @throws NodeProcessException if unable to create the callbacks.
     */
    private List<Callback> createScanQRCodeCallbacks(TreeContext context, OathDeviceSettings deviceProfile,
                                                     AMIdentity identity) throws NodeProcessException {
        Map<String, String> params = buildURIParameters(deviceProfile);

        String message = localizationHelper.getLocalizedMessage(context, OathRegistrationNode.class,
                config.scanQRCodeMessage(), SCAN_QR_CODE_MSG_KEY);
        Callback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, message);
        Callback hiddenCallback = createHiddenCallback(identity, params);
        Callback qrCodeCallback = createQRCodeCallback(identity, params);

        String nextLabelText = localizationHelper.getLocalizedMessageWithDefault(context, OathRegistrationNode.class,
                null, NEXT_LABEL_KEY_NAME, NEXT_LABEL);

        return ImmutableList.of(
                textOutputCallback,
                qrCodeCallback,
                hiddenCallback,
                new ConfirmationCallback(ConfirmationCallback.YES, new String[]{nextLabelText}, NEXT_OPTION)
        );
    }

    /**
     * Creates the QRCode callback.
     *
     * @param identity the AM identity.
     * @param params the query parameters map.
     * @return The QRCode callback.
     */
    @VisibleForTesting
    Callback createQRCodeCallback(AMIdentity identity, Map<String, String> params) {
        return createQRCodeCallback(
                OATH_URI_SCHEME_QR_CODE_KEY,
                config.algorithm() == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                getUserAttributeForAccountName(identity, config.accountName().toString()),
                0,
                params
        );
    }

    /**
     * Creates the Hidden callback.
     *
     * @param identity the AM identity.
     * @param params the query parameters map.
     * @return The HiddenValue callback.
     */
    @VisibleForTesting
    Callback createHiddenCallback(AMIdentity identity, Map<String, String> params) {
        return createHiddenValueCallback(
                HIDDEN_CALLCABK_ID,
                OATH_URI_SCHEME_QR_CODE_KEY,
                config.algorithm() == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                getUserAttributeForAccountName(identity, config.accountName().toString()),
                params
        );
    }

    /**
     * Build the URI parameters to create the QR Code.
     *
     * @param deviceProfile the Oath device's settings.
     * @return the map of parameters.
     * @throws NodeProcessException if unable to decode secret.
     */
    @VisibleForTesting
    Map<String, String> buildURIParameters(OathDeviceSettings deviceProfile)
            throws NodeProcessException {
        Map<String, String> params = new LinkedHashMap<>();

        try {
            params.put(SECRET_QR_CODE_KEY, getBase32Secret(deviceProfile.getSharedSecret()));
            params.put(ISSUER_QR_CODE_KEY, UrlEscapers.urlFragmentEscaper().escape(config.issuer()));

            if (config.algorithm() == OathAlgorithm.HOTP) {
                params.put(COUNTER_QR_CODE_KEY, String.valueOf(deviceProfile.getCounter()));
            } else if (config.algorithm() == OathAlgorithm.TOTP) {
                params.put(PERIOD_QR_CODE_KEY, String.valueOf(config.totpTimeInterval()));
            } else {
                throw new NodeProcessException("No OTP algorithm selected");
            }

            params.put(DIGITS_QR_CODE_KEY, String.valueOf(config.passwordLength()));

            if (config.bgColor() != null && config.bgColor().startsWith("#")) {
                params.put(BGCOLOUR_QR_CODE_KEY, config.bgColor().substring(1));
            } else {
                params.put(BGCOLOUR_QR_CODE_KEY, config.bgColor());
            }

            if (config.imgUrl() != null) {
                params.put(IMG_QR_CODE_KEY, config.imgUrl());
            }

            if ((config.algorithm() == OathAlgorithm.TOTP)
                    && (config.totpHashAlgorithm() != HashAlgorithm.HMAC_SHA1)) {
                params.put(ALGORITHM_QR_CODE_KEY, config.totpHashAlgorithm().toString());
            }
        } catch (DecoderException e) {
            throw new NodeProcessException("Could not decode secret key from hex to plain text", e);
        }

        return params;
    }

    @VisibleForTesting
    String getBase32Secret(String secret) throws DecoderException {
        byte[] secretPlainTextBytes = Hex.decodeHex(secret.toCharArray());
        Base32 base32 = new Base32();
        return new String(base32.encode(secretPlainTextBytes));
    }

    /**
     * Set the current user should not skip MFA.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     */
    @VisibleForTesting
    void setUserToNotSkippable(AMIdentity amIdentity, String realm) {
        setUserSkip(amIdentity, realm, SkipSetting.NOT_SKIPPABLE);
    }

    /**
     * The OATH Registration node configuration.
     */
    public interface Config {

        /**
         * Specifies the name of the issuer.
         *
         * @return issuer name as string.
         */
        @Attribute(order = 10, validators = {RequiredValueValidator.class})
        default String issuer() {
            return DEFAULT_ISSUER;
        }

        /**
         * Specifies the attribute to be used as account name.
         *
         * @return account name as string.
         */
        @Attribute(order = 20)
        default UserAttributeToAccountNameMapping accountName() {
            return UserAttributeToAccountNameMapping.USERNAME;
        }

        /**
         * Background color of entry in ForgeRock Authenticator app.
         *
         * @return the hexadecimal color value as string.
         */
        @Attribute(order = 30)
        default String bgColor() {
            return DEFAULT_BG_COLOR;
        }

        /**
         * URL of a logo image resource associated with the Issuer.
         *
         * @return the URL of the logo image resource.
         */
        @Attribute(order = 40)
        default String imgUrl() {
            return "";
        }

        /**
         * Specifies whether to generate recovery codes and store them in the device profile.
         *
         * @return true if the codes are to be generated.
         */
        @Attribute(order = 50)
        default boolean generateRecoveryCodes() {
            return DEFAULT_GENERATE_RECOVERY_CODES;
        }

        /**
         * Specifies the One Time Password length.
         *
         * @return the length of the generated OTP in digits.
         */
        @Attribute(order = 60)
        default NumberOfDigits passwordLength() {
            return NumberOfDigits.SIX_DIGITS;
        }

        /**
         * Specifies the Minimum Secret Key Length.
         *
         * @return the number of hexadecimal characters allowed for the Secret Key.
         */
        @Attribute(order = 70)
        default int minSharedSecretLength() {
            return DEFAULT_MIN_SHARED_SECRET_LENGTH;
        }

        /**
         * Specifies the OATH Algorithm to Use.
         *
         * @return the algorithm the device uses to generate the OTP.
         */
        @Attribute(order = 80)
        default OathAlgorithm algorithm() {
            return OathAlgorithm.TOTP;
        }

        /**
         * Specifies the TOTP time step interval.
         *
         * @return the TOTP time step in seconds that the OTP device uses to generate the OTP.
         */
        @Attribute(order = 90)
        default int totpTimeInterval() {
            return DEFAULT_TOTP_INTERVAL;
        }

        /**
         * Specifies the TOTP hash algorithm.
         *
         * @return the TOTP hash algorithm to be used to generate the OTP.
         */
        @Attribute(order = 100)
        default HashAlgorithm totpHashAlgorithm() {
            return HashAlgorithm.HMAC_SHA1;
        }

        /**
         * Specifies if should add Checksum Digit.
         *
         * @return true if it adds a checksum digit to the OTP.
         */
        @Attribute(order = 110)
        default boolean addChecksum() {
            return DEFAULT_CHECKSUM;
        }

        /**
         * Specifies the Truncation Offset.
         *
         * @return the value of an offset to the generation of the OTP.
         */
        @Attribute(order = 120)
        default int truncationOffset() {
            return DEFAULT_TRUNCATION_OFFSET;
        }

        /**
         * The message to displayed to user to scan the QR code.
         * @return The mapping of locales to scan QR code messages.
         */
        @Attribute(order = 130)
        default Map<Locale, String> scanQRCodeMessage() {
            return Collections.emptyMap();
        }
    }

    /**
     * Provides the oath registration node's set of outcomes.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    OathRegistrationNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID))
            );
        }
    }

}
