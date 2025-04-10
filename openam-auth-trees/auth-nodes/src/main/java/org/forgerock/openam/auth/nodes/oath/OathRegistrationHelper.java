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

package org.forgerock.openam.auth.nodes.oath;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.BGCOLOUR_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.HIDDEN_CALLCABK_ID;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.IMG_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.SCAN_QR_CODE_MSG_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.ALGORITHM_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.COUNTER_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.DIGITS_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.HOTP_URI_HOST_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_ENABLE_RECOVERY_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_URI_SCHEME_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.PERIOD_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.SECRET_QR_CODE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.TOTP_URI_HOST_QR_CODE_KEY;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.net.UrlEscapers;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider.LocalizedMessageProviderFactory;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationUtilities;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used for nodes performing OATH Registration, shares common features and components across the
 * multiple push-related nodes.
 */
public class OathRegistrationHelper {

    private static final Logger logger = LoggerFactory.getLogger(OathRegistrationHelper.class);

    /** The value for the Next option. */
    public static final int NEXT_OPTION = 0;
    /** The label for the Next option. */
    public static final String NEXT_LABEL = "Next";
    private static final String NEXT_LABEL_KEY_NAME = "nextButtonLabel";

    private final Realm realm;
    private final OathDeviceProfileHelper deviceProfileHelper;
    private final MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate;
    private final LocalizedMessageProvider localizationHelper;
    private final MultiFactorRegistrationUtilities multiFactorRegistrationUtilities;

    /**
     * The Oath registration helper constructor.
     *
     * @param realm                            the realm.
     * @param deviceProfileHelper              stores device profiles.
     * @param multiFactorNodeDelegate          shared utilities common to second factor implementations.
     * @param localizationHelperFactory        the localization helper factory.
     * @param multiFactorRegistrationUtilities shared utilities for mfa registration operations.
     */
    @Inject
    public OathRegistrationHelper(
            @Assisted Realm realm,
            OathDeviceProfileHelper deviceProfileHelper,
            MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate,
            LocalizedMessageProviderFactory localizationHelperFactory,
            MultiFactorRegistrationUtilities multiFactorRegistrationUtilities) {
        this.realm = realm;
        this.deviceProfileHelper = deviceProfileHelper;
        this.multiFactorNodeDelegate = multiFactorNodeDelegate;
        this.localizationHelper = localizationHelperFactory.create(realm);
        this.multiFactorRegistrationUtilities = multiFactorRegistrationUtilities;
    }

    /**
     * Cleanup the SharedState.
     *
     * @param context                      the context of the tree authentication.
     * @param builder                      the ActionBuilder.
     * @param postponeDeviceProfileStorage indicates if it should postpone the device settings storage.
     * @return the same instance of the ActionBuilder.
     */
    public Action.ActionBuilder cleanupSharedState(TreeContext context,
                                                   Action.ActionBuilder builder,
                                                   boolean postponeDeviceProfileStorage) {
        if (postponeDeviceProfileStorage) {
            return builder.replaceSharedState(context.sharedState);
        } else {
            JsonValue sharedState = context.sharedState.copy();
            sharedState.remove(OATH_DEVICE_PROFILE_KEY);
            sharedState.remove(OATH_ENABLE_RECOVERY_CODE_KEY);
            return builder.replaceSharedState(sharedState);
        }
    }

    /**
     * Remove oath entries from given SharedState.
     *
     * @param nodeState the Node state representation object.
     */
    public void clearSharedState(NodeState nodeState) {
        nodeState.remove(OATH_DEVICE_PROFILE_KEY);
        nodeState.remove(OATH_ENABLE_RECOVERY_CODE_KEY);
    }

    /**
     * Set push entries used for registration to the SharedState.
     *
     * @param nodeState             the Node state representation object.
     * @param oathDeviceSettings    the Push device's settings.
     * @param generateRecoveryCodes indicates if it should create recovery codes.
     * @throws IOException if unable to update the shared state.
     */
    public void updateSharedState(NodeState nodeState,
                                  OathDeviceSettings oathDeviceSettings,
                                  boolean generateRecoveryCodes) throws IOException {
        nodeState.putShared(OATH_DEVICE_PROFILE_KEY, deviceProfileHelper.encodeDeviceSettings(oathDeviceSettings));
        nodeState.putShared(OATH_ENABLE_RECOVERY_CODE_KEY, generateRecoveryCodes);
    }

    /**
     * Checks if Device Profile is stored.
     *
     * @param username the username of user
     * @return true if already stored, false otherwise.
     */
    public boolean isDeviceSettingsStored(String username) {
        return this.deviceProfileHelper.isDeviceSettingsStored(username);
    }

    /**
     * Return the OATH device settings object stored on the shared state.
     *
     * @param context the tree context.
     * @return the OATH device settings object.
     * @throws NodeProcessException if unable to retrieve device profile from shared state.
     */
    public OathDeviceSettings getDeviceProfileFromSharedState(TreeContext context) throws NodeProcessException {
        return deviceProfileHelper.getDeviceProfileFromSharedState(context, OATH_DEVICE_PROFILE_KEY);
    }

    /**
     * Creates a OATH registration URI based on the device settings and node configuration.
     *
     * @param identity       the user identity.
     * @param deviceSettings the device's settings.
     * @param config         the node configuration.
     * @return the OATH URI.
     * @throws NodeProcessException if an error occur building the URI.
     */
    public String createRegistrationUri(AMIdentity identity, OathDeviceSettings deviceSettings,
                                        OathRegistrationConfig config)
            throws NodeProcessException {
        Map<String, String> params = buildURIParameters(deviceSettings, config);

        return multiFactorRegistrationUtilities.createUri(OATH_URI_SCHEME_QR_CODE_KEY,
                config.algorithm() == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(config.issuer()),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(identity,
                        config.accountName().toString()),
                params);
    }

    /**
     * Creates a fresh OATH device profile.
     *
     * @param config the node configuration.
     * @return the OATH device profile.
     */
    public OathDeviceSettings createDeviceSettings(OathRegistrationConfig config) {
        return deviceProfileHelper.createDeviceSettings(config.minSharedSecretLength(),
                config.addChecksum(), config.truncationOffset());
    }

    /**
     * Save the device's settings on the user's profile.
     *
     * @param deviceSettings        the device's settings.
     * @param userIdentity          the user identity.
     * @param generateRecoveryCodes indicates if it should create recovery codes.
     * @return list of recovery codes.
     * @throws NodeProcessException if unable to store device profile or generate the recovery codes.
     */
    public List<String> saveDeviceSettings(OathDeviceSettings deviceSettings, AMIdentity userIdentity,
                                           boolean generateRecoveryCodes) throws NodeProcessException {
        List<String> recoveryCodes = deviceProfileHelper.saveDeviceSettings(deviceSettings,
                json(object()), userIdentity, generateRecoveryCodes);
        setUserToNotSkippable(userIdentity);
        return recoveryCodes;
    }

    /**
     * Save the device's settings on the user's profile.
     *
     * @param deviceSettings the device's settings.
     * @param userIdentity   the user identity.
     * @param recoveryCodes  list of recovery codes.
     * @throws NodeProcessException if unable to store device profile.
     */
    public void saveDeviceSettings(OathDeviceSettings deviceSettings,
                                   AMIdentity userIdentity,
                                   List<String> recoveryCodes) throws NodeProcessException {
        deviceProfileHelper.saveDeviceSettings(deviceSettings, userIdentity, recoveryCodes);
        setUserToNotSkippable(userIdentity);
    }

    /**
     * Creates the QRCode callback.
     *
     * @param identity      the AM identity.
     * @param params        the query parameters map.
     * @param algorithm     the OATH algorithm type.
     * @param issuer        the issuer.
     * @param userAttribute the user attribute.
     * @return The QRCode callback.
     */
    public Callback createQRCodeCallback(AMIdentity identity, Map<String, String> params, OathAlgorithm algorithm,
                                         String issuer, String userAttribute) {
        return multiFactorRegistrationUtilities.createQRCodeCallback(
                OATH_URI_SCHEME_QR_CODE_KEY,
                algorithm == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(issuer),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(identity, userAttribute),
                0,
                params
        );
    }

    /**
     * Creates the Hidden callback.
     *
     * @param identity      the AM identity.
     * @param params        the query parameters map.
     * @param algorithm     the OATH algorithm type.
     * @param issuer        the issuer.
     * @param userAttribute the user attribute.
     * @return The HiddenValue callback.
     */
    public Callback createHiddenCallback(AMIdentity identity, Map<String, String> params, OathAlgorithm algorithm,
                                         String issuer, String userAttribute) {
        return multiFactorRegistrationUtilities.createHiddenValueCallback(
                HIDDEN_CALLCABK_ID,
                OATH_URI_SCHEME_QR_CODE_KEY,
                algorithm == OathAlgorithm.HOTP ? HOTP_URI_HOST_QR_CODE_KEY : TOTP_URI_HOST_QR_CODE_KEY,
                UrlEscapers.urlFragmentEscaper().escape(issuer),
                multiFactorRegistrationUtilities.getUserAttributeForAccountName(identity, userAttribute),
                params
        );
    }

    /**
     * Creates a localized TextOutput callback.
     *
     * @param context           the context of the tree authentication.
     * @param bundleClass       the bundle class.
     * @param scanQRCodeMessage localized scan QR Code message.
     * @return the textOutput callback.
     */
    public Callback createLocalizedTextCallback(TreeContext context, Class<?> bundleClass,
                                                Map<Locale, String> scanQRCodeMessage) {
        String message = localizationHelper.getLocalizedMessage(context, bundleClass,
                scanQRCodeMessage, SCAN_QR_CODE_MSG_KEY);
        return new TextOutputCallback(TextOutputCallback.INFORMATION, message);
    }

    /**
     * Creates a localized Confirmation callback, using the default labelText if no localized value is found.
     *
     * @param context           the context of the tree authentication.
     * @param bundleClass       the bundle class.
     * @return the textOutput callback.
     */
    public Callback createLocalizedConfirmationCallback(TreeContext context, Class<?> bundleClass) {
        String nextLabelText = localizationHelper.getLocalizedMessageWithDefault(context, bundleClass,
                null, NEXT_LABEL_KEY_NAME, NEXT_LABEL);
        return new ConfirmationCallback(ConfirmationCallback.YES, new String[]{nextLabelText}, NEXT_OPTION);
    }

    /**
     * Build the OATH URI parameters to create the QR Code.
     *
     * @param deviceProfile the Oath device's settings.
     * @param config        the OATH registration configuration.
     * @return the map of parameters.
     * @throws NodeProcessException if unable to decode secret.
     */
    public Map<String, String> buildURIParameters(OathDeviceSettings deviceProfile, OathRegistrationConfig config)
            throws NodeProcessException {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(SECRET_QR_CODE_KEY, getBase32Secret(deviceProfile.getSharedSecret()));

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

        return params;
    }

    /**
     * Set the current user should not skip MFA.
     *
     * @param amIdentity the identity of the user.
     */
    public void setUserToNotSkippable(AMIdentity amIdentity) {
        try {
            multiFactorNodeDelegate.setUserSkip(amIdentity, realm.asPath(), SkipSetting.NOT_SKIPPABLE);
        } catch (NodeProcessException e) {
            logger.debug("Unable to set user attribute as skippable.", e);
        }
    }

    /**
     * Decode Base32 secret to string.
     *
     * @param secret the secret as String.
     * @return the string decoded.
     * @throws NodeProcessException if fail to decode the String.
     */
    public String getBase32Secret(String secret) throws NodeProcessException {
        try {
            byte[] secretPlainTextBytes = Hex.decodeHex(secret.toCharArray());
            Base32 base32 = new Base32();
            return new String(base32.encode(secretPlainTextBytes));
        } catch (DecoderException e) {
            throw new NodeProcessException("An unexpected error occurred while decoding the string.", e);
        }
    }

}
