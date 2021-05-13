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
 * Copyright 2012-2019 ForgeRock AS.
 * Portions Copyrighted 2014-2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.fr.oath;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.DecoderException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.rest.authn.mobile.TwoFactorAMLoginModule;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.qr.GenerationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;
import com.sun.identity.authentication.modules.hotp.OTPGenerator;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;

/**
 * Implements the OATH specification. OATH uses a OTP to authenticate
 * a token to the server. This class implements two of OATH's protocols for OTP
 * generation and authentication; HMAC-based One Time Password (HOTP) and
 * Time-based One Time Password (TOTP).
 */
public class AuthenticatorOATH extends TwoFactorAMLoginModule {

    //debug log name
    private Logger debug = null;

    private String userId = null;
    private String userName = null;

    //static attribute names
    private static final int NUM_CODES = 10;

    private static final String PASSWORD_LENGTH =
            "iplanet-am-auth-fr-oath-password-length";
    private static final String WINDOW_SIZE =
            "iplanet-am-auth-fr-oath-hotp-window-size";
    private static final String TRUNCATION_OFFSET =
            "iplanet-am-auth-fr-oath-truncation-offset";
    private static final String CHECKSUM = "iplanet-am-auth-fr-oath-add-checksum";
    private static final String TOTP_TIME_STEP =
            "iplanet-am-auth-fr-oath-size-of-time-step";
    private static final String TOTP_STEPS_IN_WINDOW =
            "iplanet-am-auth-fr-oath-steps-in-window";
    private static final String ALGORITHM = "iplanet-am-auth-fr-oath-algorithm";
    private static final String MIN_SECRET_KEY_LENGTH =
            "iplanet-am-auth-fr-oath-min-secret-key-length";
    private static final String MAXIMUM_CLOCK_DRIFT = "openam-auth-fr-oath-maximum-clock-drift";
    private static final String ISSUER_NAME = "openam-auth-fr-oath-issuer-name";
    private static final String FRAUTH_RETRY = "openam-auth-fr-oath-max-retry";

    private static final String MODULE_NAME = "ForgeRock Authenticator (OATH)";

    //module attribute holders
    private String issuerName;
    private int passLen = 0;
    private int minSecretKeyLength = 0;
    private int windowSize = 0;
    private int truncationOffset = -1;
    private boolean checksum = false;
    private int totpTimeStep = 0;
    private int totpStepsInWindow = 0;
    private long time = 0;
    private int totpMaxClockDrift = 0;
    private int attempt = 0;
    private static int totalAttempts = 0;

    private static final int HOTP = 0;
    private static final int TOTP = 1;
    private static final int ERROR = 2;
    private int algorithm = 0;

    private boolean outOfSync = false;

    protected String amAuthOATH = null;

    private static final int LOGIN_START = ISAuthConstants.LOGIN_START;
    private static final int LOGIN_OPTIONAL = 2;
    private static final int LOGIN_NO_DEVICE = 3;
    private static final int LOGIN_SAVED_DEVICE = 4;
    private static final int REGISTER_DEVICE = 5;
    private static final int RECOVERY_USED = 6;
    private static final int LOGIN_OPT_DEVICE = 7;
    private static final int SHOW_RECOVERY_CODES = 8;

    private static final int REGISTER_DEVICE_OPTION_VALUE_INDEX = 0;
    private static final int OPT_DEVICE_SKIP_INDEX = 1;
    private static final int SKIP_OATH_INDEX = 1;

    private static final int SCRIPT_OUTPUT_CALLBACK_INDEX = 1;

    private AMIdentity id;

    private final UserOathDeviceProfileManager oathDevices = InjectorHolder.getInstance(UserOathDeviceProfileManager.class);

    private final AuthenticatorDeviceServiceFactory<AuthenticatorOathService> oathServiceFactory =
            InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<AuthenticatorDeviceServiceFactory<AuthenticatorOathService>>(){}));

    private final RecoveryCodeGenerator recoveryCodeGenerator = InjectorHolder.getInstance(RecoveryCodeGenerator.class);

    private OathDeviceSettings newDevice = null;

    // support for search with alias name
    private Set<String> userSearchAttributes = Collections.emptySet();

    private OTPGenerator hotpGenerator;

    /**
     * Standard constructor sets-up the debug logging module.
     */
    public AuthenticatorOATH() {
        amAuthOATH = "amAuthAuthenticatorOATH";
        debug = LoggerFactory.getLogger(amAuthOATH);
        this.hotpGenerator = new HOTPAlgorithm();
    }

    /**
     * Returns the principal for this module. This class is overridden from
     * AMLoginModule.
     *
     * @return Principal of the authenticated user.
     */
    @Override
    public java.security.Principal getPrincipal() {
        if (userId != null) {
            return new OATHPrincipal(userId);
        }
        if (userName != null) {
            return new OATHPrincipal(userName);
        }
        return null;
    }

    /**
     * Initializes the authentication module. This function gets the modules
     * settings, and the username from the previous authentication module in
     * the chain.
     *
     * @param subject For whom this module is initializing.
     * @param sharedState Previously chained module data.
     * @param options Configuration for this module.
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        debug.debug("AuthenticatorOATH::init");

        userName = (String) sharedState.get(getUserKey());
        try {
            checkForSessionAndGetUsernameAndUUID();
        } catch (AuthLoginException | SSOException e) {
            debug.debug("AuthenticatorOATH :: init() : Unable to get userName ", e);
            return;
        }

        try {
            userSearchAttributes = getUserAliasList();
        } catch (final AuthLoginException ale) {
            debug.warn("AuthenticatorOATH :: init() : Unable to retrieve search attributes", ale);
        }

        try {

            String realm = DNMapper.orgNameToRealmName(getRequestOrg());
            id = IdUtils.getIdentity(userName, realm, userSearchAttributes);
            realmService = oathServiceFactory.create(id.getRealm());

            try {
                this.passLen = CollectionHelper.getIntMapAttr(options, PASSWORD_LENGTH, 0, debug);
            } catch (NumberFormatException e) {
                passLen = 0;
            }

            try {
                this.minSecretKeyLength = CollectionHelper.getIntMapAttr(options, MIN_SECRET_KEY_LENGTH, 0, debug);
            } catch (NumberFormatException e) {
                minSecretKeyLength = 0;
            }

            this.windowSize = CollectionHelper.getIntMapAttr(options, WINDOW_SIZE, 0, debug);
            this.truncationOffset = CollectionHelper.getIntMapAttr(options, TRUNCATION_OFFSET, -1, debug);
            this.totpTimeStep = CollectionHelper.getIntMapAttr(options, TOTP_TIME_STEP, 1, debug);
            this.totpStepsInWindow = CollectionHelper.getIntMapAttr(options, TOTP_STEPS_IN_WINDOW, 1, debug);
            this.checksum = CollectionHelper.getBooleanMapAttr(options, CHECKSUM, false);
            this.totpMaxClockDrift = CollectionHelper.getIntMapAttr(options, MAXIMUM_CLOCK_DRIFT, 0, debug);
            this.issuerName = CollectionHelper.getMapAttr(options, ISSUER_NAME);
            this.totalAttempts = CollectionHelper.getIntMapAttr(options, FRAUTH_RETRY, 3, debug);

            final String algorithm = CollectionHelper.getMapAttr(options, ALGORITHM);
            if ("HOTP".equalsIgnoreCase(algorithm)) {
                this.algorithm = HOTP;
            } else if ("TOTP".equalsIgnoreCase(algorithm)) {
                this.algorithm = TOTP;
            } else {
                this.algorithm = ERROR;
            }

            try {
                isSkippable = isOptional(realmService);

                if (isSkippable) {
                    userConfiguredSkippable = userConfiguredSkippable(id, realmService);
                }
            } catch (IdRepoException e) {
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }

        } catch (SMSException | SSOException | AuthLoginException e) {
            debug.error("AuthenticatorOATH :: init() : Unable to configure basic module properties.", e);
            return;
        }

    }

    /**
     * Processes the OTP input by the user. Checks the OTP for validity, and
     * resynchronizes the server as needed.
     *
     * @param callbacks Incoming from the UI.
     * @param state State of the module to process this access.
     * @return -1 for success; 0 for failure, any other int to move to that state.
     * @throws AuthLoginException upon any errors.
     */
    @Override
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        try {
            if (StringUtils.isEmpty(userName)) {
                throw new AuthLoginException("amAuth", "noUserName", null);
            }
            final OathDeviceSettings settings = getOathDeviceSettings(id.getName(), id.getRealm());

            int selectedIndex;

            switch (state) {
                case LOGIN_OPTIONAL:
                case LOGIN_NO_DEVICE:
                case LOGIN_OPT_DEVICE:
                case LOGIN_SAVED_DEVICE:
                    if (null == callbacks) {
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }
            }

            //fall-throughs are INTENTIONAL
            switch (state) {
                case LOGIN_START:
                    return beginLogin(settings);

                case LOGIN_OPTIONAL:
                    selectedIndex = ((ConfirmationCallback) callbacks[0]).getSelectedIndex();
                    if (selectedIndex == SKIP_OATH_INDEX) {
                        realmService.setUserSkip(id, SkipSetting.SKIPPABLE);
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }
                    //fall through

                case LOGIN_NO_DEVICE:
                    selectedIndex = ((ConfirmationCallback) callbacks[0]).getSelectedIndex();
                    if (selectedIndex == REGISTER_DEVICE_OPTION_VALUE_INDEX) {
                        newDevice = createBasicDevice();
                        paintRegisterDeviceCallback(id, newDevice);
                        return REGISTER_DEVICE;
                    }
                    //fall through

                case LOGIN_OPT_DEVICE:
                    selectedIndex = ((ConfirmationCallback) callbacks[1]).getSelectedIndex();
                    if (selectedIndex == OPT_DEVICE_SKIP_INDEX) {
                        realmService.setUserSkip(id, SkipSetting.SKIPPABLE);
                        realmService.removeAllUserDevices(id); //user backed out of saving device
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }
                    //fall through

                case LOGIN_SAVED_DEVICE:
                    return doLoginSavedDevice(callbacks, state, settings);

                case REGISTER_DEVICE:
                    if (isSkippable) {
                        replaceHeader(LOGIN_OPT_DEVICE, MODULE_NAME);
                        return LOGIN_OPT_DEVICE;
                    } else {
                        replaceHeader(LOGIN_SAVED_DEVICE, MODULE_NAME);
                        return LOGIN_SAVED_DEVICE;
                    }

                case SHOW_RECOVERY_CODES:
                    return ISAuthConstants.LOGIN_SUCCEED;

                case RECOVERY_USED:
                    if (isSkippable) { //if it's skippable and you log in, config not skippable
                        realmService.setUserSkip(id, SkipSetting.NOT_SKIPPABLE);
                    }
                    return ISAuthConstants.LOGIN_SUCCEED;

                default:
                    throw new AuthLoginException("amAuth", "invalidLoginState", new Object[]{state});
            }
        } catch (SSOException | IdRepoException | DevicePersistenceException e) {
            debug.error("AuthenticatorOATH.process() : SSOException", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
    }

    private void checkForSessionAndGetUsernameAndUUID() throws SSOException, AuthLoginException {
        if (StringUtils.isEmpty(userName)) {
            // session upgrade case. Need to find the user ID from the old
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            Session isess = getLoginState("OATH").getOldSession();
            if (isess == null) {
                throw new AuthLoginException("amAuth", "noInternalSession", null);
            }
            SSOToken token = mgr.createSSOToken(isess.getID().toString());
            userId = token.getPrincipal().getName();
            userName = token.getProperty("UserToken");
            if (debug.isDebugEnabled()) {
                debug.debug("OATH.process() : Username from SSOToken : " + userName);
            }
            if (StringUtils.isEmpty(userName)) {
                throw new AuthLoginException("amAuth", "noUserName", null);
            }
        }
    }

    private int beginLogin(OathDeviceSettings settings) throws AuthLoginException {
        if (isSkippable && userConfiguredSkippable == SkipSetting.SKIPPABLE) {
            return ISAuthConstants.LOGIN_SUCCEED;
        } else if (isSkippable && userConfiguredSkippable == SkipSetting.NOT_SET) {
            return LOGIN_OPTIONAL;
        } else if (isSkippable && userConfiguredSkippable != SkipSetting.NOT_SKIPPABLE) {
            throw new AuthLoginException(amAuthOATH, "authFailed", null); //invalid so error
        } else {
            if (settings == null) {
                if (isSkippable) {
                    return LOGIN_OPTIONAL;
                } else {
                    return LOGIN_NO_DEVICE;
                }
            } else {
                if (isSkippable) {
                    replaceHeader(LOGIN_OPT_DEVICE, MODULE_NAME);
                    return LOGIN_OPT_DEVICE;
                } else {
                    replaceHeader(LOGIN_SAVED_DEVICE, MODULE_NAME);
                    return LOGIN_SAVED_DEVICE;
                }
            }
        }
    }

    private int doLoginSavedDevice(final Callback[] callbacks, final int state, final OathDeviceSettings settings)
            throws AuthLoginException, IdRepoException, SSOException, DevicePersistenceException {

        OathDeviceSettings deviceToAuthAgainst = settings;

        if (null == deviceToAuthAgainst && null != newDevice) {
            deviceToAuthAgainst = newDevice;
        }

        //get OTP
        String OTP = ((NameCallback) callbacks[0]).getName();
        if (OTP.length() == 0) {
            debug.error("AuthenticatorOATH.process() : invalid OTP code");
            if (++attempt >= totalAttempts) {
                setFailureID(userName);
                throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
            }

            replaceHeader(state, MODULE_NAME + "Attempt " + (attempt + 1) + " of " + totalAttempts);
            return state;
        }

        //get Arrival time of the OTP
        time = currentTimeMillis() / 1000L;

        if (isRecoveryCode(OTP, deviceToAuthAgainst, id)) {
            return RECOVERY_USED;
        } else if (checkOTP(OTP, id, deviceToAuthAgainst)) {
            if (isSkippable) { //if it's skippable and you log in, config not skippable
                realmService.setUserSkip(id, SkipSetting.NOT_SKIPPABLE);
            }
            if (null == settings) {
                // this is the first time we have authorised against this device - we can now save it.
                oathDevices.saveDeviceProfile(id.getName(), id.getRealm(), deviceToAuthAgainst);
                return displayRecoveryCodes(SHOW_RECOVERY_CODES);
            }
            return ISAuthConstants.LOGIN_SUCCEED;
        } else {
            //the OTP is out of the window or incorrect
            if (++attempt >= totalAttempts) {
                setFailureID(userName);
                if (outOfSync) {
                    throw new InvalidPasswordException(amAuthOATH, "outOfSync", null);
                } else {
                    throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                }
            }

            replaceHeader(state, MODULE_NAME + "Attempt " + (attempt + 1) + " of " + totalAttempts);
            return state;
        }
    }

    private OathDeviceSettings createBasicDevice() throws AuthLoginException {

        OathDeviceSettings settings = oathDevices.createDeviceProfile(minSecretKeyLength);
        settings.setChecksumDigit(checksum);

        try {
            recoveryCodes = recoveryCodeGenerator.generateCodes(NUM_CODES, Alphabet.ALPHANUMERIC, false);
            settings.setRecoveryCodes(recoveryCodes);
        } catch (CodeException e) {
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        return settings;
    }

    private boolean isRecoveryCode(String otp, OathDeviceSettings settings, AMIdentity id)
            throws AuthLoginException, DevicePersistenceException {
        if (settings == null) {
            debug.error("AuthenticatorOATH.checkOTP() : Invalid stored settings.");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        if (settings.useRecoveryCode(otp)) {
            oathDevices.saveDeviceProfile(id.getName(), id.getRealm(), settings);
            return true;
        }

        return false;
    }

    private void paintRegisterDeviceCallback(AMIdentity id, OathDeviceSettings settings) throws AuthLoginException {
        replaceCallback(REGISTER_DEVICE, SCRIPT_OUTPUT_CALLBACK_INDEX, createQRCodeCallback(settings, id,
                SCRIPT_OUTPUT_CALLBACK_INDEX));
    }

    /**
    * There is a hack here to reverse a hack in RESTLoginView.js. Implementing the code properly in RESTLoginView.js so
    * as to remove this hack will take too long at present, and stands in the way of completion of this module's
    * QR code additions. I have opted to simply reverse the hack in this singular case.
    *
    * In the below code returning the ScriptTextOutputCallback, the String used in its construction is
    * defined as follows:
     *
    * createQRDomElementJS
    *           Adds the DOM element, in this case a div, in which the QR code will appear.
    * QRCodeGenerationUtilityFunctions.
    *   getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(authenticatorAppRegistrationUri)
    *           Adds a specific call to the Javascript library code, sending the app registration url as the
    *           text to encode as a QR code. This QR code will then appear in the previously defined DOM
    *           element (which must have an id of 'qr').
    * hideButtonHack
    *           A hack to reverse a hack in RESTLoginView.js. See more detailed comment above.*
    */
    private Callback createQRCodeCallback(OathDeviceSettings settings, AMIdentity id, int callbackIndex)
            throws AuthLoginException {
        final String authenticatorAppRegistrationUri = getAuthenticatorAppRegistrationUri(settings, id);
        final String callback = "callback_" + callbackIndex;
        return new ScriptTextOutputCallback(
                GenerationUtils.getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(callback,
                        authenticatorAppRegistrationUri));
    }

    private String getAuthenticatorAppRegistrationUri(OathDeviceSettings settings, AMIdentity id) throws
            AuthLoginException {

        //check settings aren't null
        if (settings == null) {
            debug.error("AuthenticatorOATH.checkOTP() : Invalid settings discovered.");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        final AuthenticatorAppRegistrationURIBuilder builder =
                new AuthenticatorAppRegistrationURIBuilder(id, settings.getSharedSecret(), passLen, issuerName);

        int algorithm = this.algorithm;
        try {
            if (algorithm == HOTP) {
                int counter = settings.getCounter();
                return builder.getAuthenticatorAppRegistrationUriForHOTP(counter);
            } else if (algorithm == TOTP) {
                return builder.getAuthenticatorAppRegistrationUriForTOTP(totpTimeStep);
            } else {
                debug.error("AuthenticatorOATH.checkOTP() : No OTP algorithm selected");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
        } catch (DecoderException de) {
            debug.error("AuthenticatorOATH.getAuthenticatorAppRegistrationUri() : Could not decode secret key from "
                    + "hex to plain text", de);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
    }

    /**
     * Called to cleanup the class level variables.
     */
    @Override
    public void destroyModuleState() {
        userId = null;
        userName = null;
    }

    /**
     * Called to cleanup the class level variables that won't be used again.
     */
    @Override
    public void nullifyUsedVars() {
        amAuthOATH = null;
    }

    /**
     * Checks the input OTP.
     *
     * @param otp The OTP to verify.
     * @param id The user for whom to verify the OTP.
     * @param settings With which the OTP was configured.
     * @return true if the OTP is valid; false if the OTP is invalid, or out of
     *         sync with server.
     * @throws AuthLoginException on any error
     */
    private boolean checkOTP(String otp, AMIdentity id, OathDeviceSettings settings) throws AuthLoginException {

        //check settings aren't null
        if (settings == null) {
            debug.error("AuthenticatorOATH.checkOTP() : Invalid stored settings.");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String secretKey = parseSecretKey(settings.getSharedSecret());

        if (minSecretKeyLength <= 0) {
            debug.error("AuthenticatorOATH.checkOTP() : Min Secret Key Length is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //check size of key
        if (secretKey == null || secretKey.isEmpty()) {
            debug.error("AuthenticatorOATH.checkOTP() : Secret key is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //make sure secretkey is not smaller than minSecretKeyLength
        if (secretKey.length() < minSecretKeyLength) {
            debug.error("AuthenticatorOATH.checkOTP() : Secret key of length {} is less than the minimum secret "
                    + "key length", secretKey.length());
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //convert secretkey hex string to hex.     
        byte[] secretKeyBytes = DatatypeConverter.parseHexBinary(secretKey);

        //check password length MUST be 6 or higher according to RFC
        if (passLen < 6) {
            debug.error("AuthenticatorOATH.checkOTP() : Password length is smaller than 6");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String otpGen;
        try {
            if (algorithm == HOTP) {
                int counter = settings.getCounter();

                //test the counter in the lookahead window
                for (int i = 0; i <= windowSize; i++) {
                    otpGen = hotpGenerator.generateOTP(secretKeyBytes, counter + i, passLen, checksum,
                            truncationOffset);
                    if (isEqual(otpGen, otp)) {
                        //OTP is correct set the counter value to counter+i (+1 for having been successful)
                        setCounterAttr(id, counter + i + 1, settings);
                        return true;
                    }
                }
            } else if (algorithm == TOTP) {
                //get Last login time
                long lastLoginTimeStep = settings.getLastLogin() / totpTimeStep;

                //Check TOTP values for validity
                if (lastLoginTimeStep < 0) {
                    debug.error("AuthenticatorOATH.checkOTP() : invalid login time value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //must be greater than 0 or we get divide by 0, and cant be negative
                if (totpTimeStep <= 0) {
                    debug.error("AuthenticatorOATH.checkOTP() : invalid TOTP time step interval : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                if (totpStepsInWindow < 0) {
                    debug.error("AuthenticatorOATH.checkOTP() : invalid TOTP steps in window value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //get Time Step
                long localTime = (time / totpTimeStep) + (settings.getClockDriftSeconds() / totpTimeStep);

                if (lastLoginTimeStep == localTime) {
                    debug.error("AuthenticatorOATH.checkOTP(): Login failed attempting to use the same OTP in same "
                            + "Time Step: {}", localTime);
                    return false;
                }

                boolean sameWindow = false;

                //check if we are in the time window to prevent 2 logins within the window using the same OTP

                if (lastLoginTimeStep >= (localTime - totpStepsInWindow) &&
                        lastLoginTimeStep <= (localTime + totpStepsInWindow)) {
                    debug.debug("AuthenticatorOATH.checkOTP() : Logging in in the same TOTP window");
                    sameWindow = true;
                }

                String passLenStr = Integer.toString(passLen);
                otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(localTime), passLenStr);

                if (isEqual(otpGen, otp)) {
                    return setLoginTime(id, localTime, settings);
                }

                for (int i = 1; i <= totpStepsInWindow; i++) {
                    long time1 = localTime + i;
                    long time2 = localTime - i;

                    //check time step after current time
                    otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(time1), passLenStr);

                    if (isEqual(otpGen, otp)) {
                        return setLoginTime(id, time1, settings);
                    }

                    //check time step before current time
                    otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(time2), passLenStr);

                    if (isEqual(otpGen, otp) && sameWindow) {
                        debug.error("AuthenticatorOATH.checkOTP() : Logging in in the same window with a OTP that is "
                                + "older than the current times OTP");
                        return false;
                    } else if (isEqual(otpGen, otp) && !sameWindow) {
                        return setLoginTime(id, time2, settings);
                    }
                }

            } else {
                debug.error("AuthenticatorOATH.checkOTP() : No OTP algorithm selected");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
        } catch (AuthLoginException e) {
            // Re-throw to avoid the catch-all block below that would log and lose the error message.
            throw e;
        } catch (Exception e) {
            debug.error("AuthenticatorOATH.checkOTP() : checkOTP process failed : ", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return false;
    }

    /**
     * Returns the first in the set of OATH device settings, or null if no
     * device settings were returned.
     */
    private OathDeviceSettings getOathDeviceSettings(String username, String realm) throws DevicePersistenceException {
        List<OathDeviceSettings> allSettings = oathDevices.getDeviceProfiles(username, realm);
        return CollectionUtils.getFirstItem(allSettings, null);
    }

    private String parseSecretKey(String secretKey) {
        //get rid of white space in string (messes with the data converter)
        secretKey = secretKey.replaceAll("\\s+", "");
        //convert secretKey to lowercase
        secretKey = secretKey.toLowerCase();
        //make sure secretkey is even length
        if ((secretKey.length() % 2) != 0) {
            secretKey = "0" + secretKey;
        }

        return secretKey;
    }

    /**
     * Sets the HOTP counter for a user.
     *
     * @param id      The user id to set the counter for.
     * @param counter The counter value to set the attribute too.
     * @param settings The settings to store the value in.
     */
    private void setCounterAttr(AMIdentity id, int counter, OathDeviceSettings settings)
            throws DevicePersistenceException {
        settings.setCounter(counter);
        oathDevices.saveDeviceProfile(id.getName(), id.getRealm(), settings);
    }

    /**
     * Sets the last login time of a user after checking that the clock drift hasn't spread too far from the config's
     * allowed settings.
     *
     * @param id   The id of the user to set the attribute of.
     * @param time The time <strong>step</strong> to set the attribute to.
     * @param settings The settings to store the value in.
     * @return {@code false} if the device is out of drift range, {@code true} if device profile has been saved.
     */
    private boolean setLoginTime(AMIdentity id, long time, OathDeviceSettings settings)
            throws DevicePersistenceException {

        // Update the observed time-step drift for resynchronisation
        long drift = time - (this.time / totpTimeStep);
        if (Math.abs(drift) > totpMaxClockDrift) {
            outOfSync = true;
            return false;
        }

        settings.setLastLogin(time * totpTimeStep, TimeUnit.SECONDS);
        settings.setClockDriftSeconds((int) drift * totpTimeStep);
        oathDevices.saveDeviceProfile(id.getName(), id.getRealm(), settings);
        return true;
    }

    /**
     * Perform time constant equality check.
     * Both values should not be null.
     *
     * @param str1 first value
     * @param str2 second vale
     * @return true if values are equal
     */
    private boolean isEqual(String str1, String str2)   {
        return MessageDigest.isEqual(str1.getBytes(), str2.getBytes());
    }
}
