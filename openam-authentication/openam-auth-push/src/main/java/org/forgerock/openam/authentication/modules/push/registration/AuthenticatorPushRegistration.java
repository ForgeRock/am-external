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
 * Copyright 2016-2023 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.push.registration;

import static org.forgerock.openam.authentication.modules.push.registration.Constants.AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.APPLE_LINK;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.APPLE_LINK_CALLBACK_INDEX;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.APP_STORE_SKIP_INDEX;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.AUTH_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.BGCOLOUR;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.BGCOLOUR_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.CHALLENGE_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.DEVICE_PUSH_WAIT_TIMEOUT;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.GET_THE_APP_OPTION;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.GOOGLE_LINK;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.GOOGLE_LINK_CALLBACK_INDEX;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.IMG_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.IMG_URL;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.ISSUER_OPTION_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.ISSUER_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.LOADBALANCER_DATA_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.MESSAGE_ID_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.NUM_RECOVERY_CODES;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.POLLING_TIME_OUTPUT_CALLBACK_INDEX;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.REG_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.SCRIPT_OUTPUT_CALLBACK_INDEX;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.SHARED_SECRET_QR_CODE_KEY;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.SKIP_THIS_STEP;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.START_REGISTRATION_OPTION;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_CONFIRMATION;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_GET_THE_APP;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_GET_THE_APP_NO_SKIP;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_OPTIONS;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_OPTIONS_NO_SKIP;
import static org.forgerock.openam.authentication.modules.push.registration.Constants.STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.COMMUNICATION_TYPE;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_ID;
import static org.forgerock.openam.services.push.PushNotificationConstants.DEVICE_TYPE;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;
import static org.forgerock.openam.services.push.PushNotificationConstants.MECHANISM_UID;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.authentication.callbacks.helpers.QRCallbackBuilder;
import org.forgerock.openam.authentication.modules.push.AbstractPushModule;
import org.forgerock.openam.authentication.modules.push.AuthenticatorPushPrincipal;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.TwoFactorEncryptedDeviceStorage;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushMessageResource;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;

/**
 * The Authenticator Push Registration Module is a registration module that does not authenticate a user but
 * allows a user already authenticated earlier in the chain to register their mobile device.
 *
 * A registering device will need to supply the following information to this module during its await state
 * via either the MessageDispatcher (local) or the CTS (cross-cluster).
 *
 * Format: JSON
 *
 * {
 *     "communicationType" : "",
 *     "deviceType" : "",
 *     "communicationId" : "",
 *     "deviceId" : "",
 *     "mechanismUid" : ""
 * }
 *
 * Translation to this format from an endpoint which received the message from the communication medium
 * should be performed by a Predicate.
 *
 * @see org.forgerock.openam.services.push.sns.SnsRegistrationPredicate
 *
 * Receiving this message should be performed by a service generated by the appropriate delegate for
 * the implementing Push Notification System.
 *
 * @see PushMessageResource
 */
public class AuthenticatorPushRegistration extends AbstractPushModule {

    private static final Logger DEBUG = LoggerFactory.getLogger(AuthenticatorPushRegistration.class);
    private static final int GET_APP_OPTIONAL_SKIP_BUTTON = 2;

    private PollingWaitAssistant pollingWaitAssistant;

    private AMIdentity amIdentityPrincipal;
    private PushDeviceSettings newDeviceRegistrationProfile;
    private String issuer;
    private long timeout;

    private String bgColour;
    private String imgUrl;

    private String appleLink;
    private String googleLink;

    private String lbCookieValue;
    private String realm;

    private TwoFactorEncryptedDeviceStorage realmPushService;
    private boolean isOptional;
    private SkipSetting userConfiguredSkippable = SkipSetting.NOT_SET;
    private final AuthenticatorDeviceServiceFactory<AuthenticatorPushService> pushServiceFactory =
            InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<AuthenticatorDeviceServiceFactory<AuthenticatorPushService>>() { }));

    private RecoveryCodeGenerator recoveryCodeGenerator = InjectorHolder.getInstance(RecoveryCodeGenerator.class);
    private final BaseURLProviderFactory baseUrlProviderFactory =
            InjectorHolder.getInstance(BaseURLProviderFactory.class);

    // @Checkstyle:off ConstantName
    private static final IdentityStoreFactory identityStoreFactory =
            InjectorHolder.getInstance(IdentityStoreFactory.class);
    // @Checkstyle:on ConstantName

    @Override
    public void init(final Subject subject, final Map sharedState, final Map options) {
        DEBUG.debug("{}::init", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);

        this.timeout = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));
        this.issuer = CollectionHelper.getMapAttr(options, ISSUER_OPTION_KEY);
        this.imgUrl = CollectionHelper.getMapAttr(options, IMG_URL);
        this.bgColour = CollectionHelper.getMapAttr(options, BGCOLOUR);
        this.appleLink = CollectionHelper.getMapAttr(options, APPLE_LINK);
        this.googleLink = CollectionHelper.getMapAttr(options, GOOGLE_LINK);

        if (bgColour != null && bgColour.startsWith("#")) {
            bgColour = bgColour.substring(1);
        }

        try {
            lbCookieValue = sessionCookies.getLBCookie(getSessionId());
        } catch (SessionException e) {
            DEBUG.warn("{} :: init() : Unable to determine loadbalancer cookie value",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
        }

        amIdentityPrincipal = establishPreauthenticatedUser(sharedState);
        pollingWaitAssistant = setUpPollingWaitCallbackAssistant(timeout);
        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());

        try {
            realmPushService = pushServiceFactory.create(realm);
        } catch (SMSException | SSOException e) {
            DEBUG.error("{} :: init() : Unable to retrieve the Push Service Factory.",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
            return;
        }

        try {
            isOptional = isOptional(realmPushService);

            if (isOptional) {
                userConfiguredSkippable = userConfiguredSkippable(amIdentityPrincipal, realmPushService);
            }
        } catch (AuthLoginException | SSOException | IdRepoException e) {
            DEBUG.error("{} :: init() : Unable to determine whether 2FA is mandatory for this realm.",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
            return;
        }

        try {
            pushService.init(realm);
        } catch (PushNotificationException e) {
            DEBUG.error("{} :: init() : Unable to initialiseService Push system.",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
            return;
        }

    }

    private PollingWaitAssistant setUpPollingWaitCallbackAssistant(long timeout) {
        if (Boolean.parseBoolean(SystemPropertiesManager.get(nearInstantProperty))) {
            return new PollingWaitAssistant(timeout, 1000, 1000, 1000);
        }

        return new PollingWaitAssistant(timeout);
    }

    private AMIdentity establishPreauthenticatedUser(final Map sharedState) {
        final String subjectName = (String) sharedState.get(getUserKey());
        final String realm = DNMapper.orgNameToRealmName(getRequestOrg());
        IdentityStore identityStore = identityStoreFactory.create(realm);
        return identityStore.getUserUsingAuthenticationUserAliases(subjectName);
    }

    @Override
    public int process(final Callback[] callbacks, final int state) throws LoginException {
        final HttpServletRequest request = getHttpServletRequest();

        if (request == null) {
            DEBUG.error("{} :: process() : Request was null.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw failedAsLoginException();
        }

        try {
            if (!userPushDeviceProfileManager.getDeviceProfiles(amIdentityPrincipal.getName(), realm).isEmpty()) {
                return ISAuthConstants.LOGIN_SUCCEED;
            }
        } catch (DevicePersistenceException dpe) {
            DEBUG.error("{} :: process() : Unable to talk to datastore.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw failedAsLoginException();
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return beginLogin();
        case STATE_OPTIONS_NO_SKIP:
            return navigateOptions(callbacks, false);
        case STATE_OPTIONS:
            return navigateOptions(callbacks, true);
        case STATE_GET_THE_APP_NO_SKIP:
            return startRegistration();
        case STATE_GET_THE_APP:
            return optionalStartRegistration(callbacks);
        case STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN:
            return awaitState();
        case STATE_CONFIRMATION:
            return ISAuthConstants.LOGIN_SUCCEED;
        default:
            DEBUG.error("{} :: process() : Invalid state.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw failedAsLoginException();
        }
    }

    private int optionalStartRegistration(Callback[] callbacks) throws AuthLoginException {
        if (isOptional) {
            if (((ConfirmationCallback) callbacks[GET_APP_OPTIONAL_SKIP_BUTTON]).getSelectedIndex()
                    == APP_STORE_SKIP_INDEX) {
                setSkippable(SkipSetting.SKIPPABLE);
                return ISAuthConstants.LOGIN_SUCCEED;
            }
        }

        return startRegistration();
    }

    private int navigateOptions(Callback[] callbacks, boolean canSkip) throws AuthLoginException {
        if (null == callbacks || callbacks.length < 1) {
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }

        switch (((ConfirmationCallback) callbacks[0]).getSelectedIndex()) {
        case START_REGISTRATION_OPTION:
            return startRegistration();
        case GET_THE_APP_OPTION:
            if (isOptional && userConfiguredSkippable == SkipSetting.NOT_SET) {
                setAppLinkCallbacks(STATE_GET_THE_APP);
                return STATE_GET_THE_APP;
            } else {
                setAppLinkCallbacks(STATE_GET_THE_APP_NO_SKIP);
                return STATE_GET_THE_APP_NO_SKIP;
            }
        case SKIP_THIS_STEP: //fallthrough is intentional in the case of false canSkip
            if (canSkip) {
                setSkippable(SkipSetting.SKIPPABLE);
                return ISAuthConstants.LOGIN_SUCCEED;
            }
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }
    }

    private void setSkippable(SkipSetting skippable) {
        try {
            realmPushService.setUserSkip(amIdentityPrincipal, skippable);
        } catch (IdRepoException | SSOException e) {
            DEBUG.debug("Unable to set skippable value on user.");
        }
    }

    private int beginLogin() {

        if (isOptional && userConfiguredSkippable == SkipSetting.SKIPPABLE) {
            return ISAuthConstants.LOGIN_SUCCEED;
        } else if (isOptional && userConfiguredSkippable == SkipSetting.NOT_SET) {
            return STATE_OPTIONS;
        } else {
            return STATE_OPTIONS_NO_SKIP;
        }

    }

    private int startRegistration() throws AuthLoginException {
        newDeviceRegistrationProfile = userPushDeviceProfileManager.createDeviceProfile();

        MessageId messageId = messageIdFactory.create(DefaultMessageTypes.REGISTER);
        String challenge = userPushDeviceProfileManager.createRandomBytes();

        paintRegisterDeviceCallback(amIdentityPrincipal, messageId.toString(), challenge);

        byte[] secret = Base64.decode(newDeviceRegistrationProfile.getSharedSecret());

        Set<Predicate> servicePredicates = new HashSet<>();

        servicePredicates.add(new SignedJwtVerificationPredicate(secret, JWT));
        servicePredicates.add(new PushMessageChallengeResponsePredicate(secret, challenge, JWT));
        try {
            Set<Predicate> predicates = pushService.getMessagePredicatesFor(realm).get(DefaultMessageTypes.REGISTER);
            if (predicates != null) {
                servicePredicates.addAll(predicates);
            }

            messagePromise = pushService.getMessageDispatcher(realm).expectLocally(messageId, servicePredicates);
        } catch (NotFoundException | PushNotificationException e) {
            DEBUG.error("Unable to read service addresses for Push Notification Service.");
            throw failedAsLoginException();
        } catch (CoreTokenException e) {
            DEBUG.warn("Unable to persist token in core token service.", e);
        }

        pollingWaitAssistant.start(messagePromise.getPromise());

        return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
    }

    private int awaitState() throws AuthLoginException {

        switch (pollingWaitAssistant.getPollingWaitState()) {
        case TOO_EARLY:
            return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
        case NOT_STARTED:
        case WAITING:
            return waitingChecks();
        case COMPLETE:
            return completeChecks();
        case TIMEOUT:
            DEBUG.warn("{} :: timeout value exceeded while waiting for response.",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        case SPAMMED:
            DEBUG.warn("{} :: too many requests sent to Auth module.  "
                    + "Client should obey wait time.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }
    }

    private int completeChecks() throws AuthLoginException {
        try {
            pushService.getMessageDispatcher(realm).forget(messagePromise.getMessageId());
        } catch (CoreTokenException e) {
            DEBUG.warn("Removing token from CTS failed.", e);
        } catch (NotFoundException e) {
            DEBUG.warn("Error finding local message dispatcher.", e);
        }
        try {
            return finaliseSuccess(messagePromise.getPromise().get());
        } catch (ExecutionException | InterruptedException e) {
            DEBUG.error("{} :: Failed to save device settings.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }
    }

    private int finaliseSuccess(JsonValue deviceResponse) throws AuthLoginException {
        storeUsername(amIdentityPrincipal.getName());
        saveDeviceDetailsUnderUserAccount(deviceResponse);
        setSkippable(SkipSetting.NOT_SKIPPABLE);
        return displayRecoveryCodes(STATE_CONFIRMATION);
    }

    private int waitingChecks() throws AuthLoginException {
        try {
            ClusterMessageHandler messageHandler = pushService.getMessageHandlers(realm)
                    .get(DefaultMessageTypes.REGISTER);
            MessageState state = messageHandler.check(messagePromise.getMessageId());

            if (state == MessageState.SUCCESS) {
                JsonValue contents = messageHandler.getContents(messagePromise.getMessageId());
                messageHandler.delete(messagePromise.getMessageId());
                return finaliseSuccess(contents);
            } else if (state == MessageState.DENIED) {
                // this should not happen, as denying a reg attempt shouldn't occur, but to be safe...
                messageHandler.delete(messagePromise.getMessageId());
                throw failedAsLoginException();
            }
        } catch (CoreTokenException e) {
            DEBUG.warn("CTS threw exception, falling back to local MessageDispatcher.", e);
        } catch (PushNotificationException e) {
            DEBUG.error("Could not find local MessageDispatcher for realm.", e);
            throw failedAsLoginException();
        }

        setPollbackTimePeriod(pollingWaitAssistant.getWaitPeriod());
        pollingWaitAssistant.resetWait();
        return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
    }

    private void saveDeviceDetailsUnderUserAccount(JsonValue deviceResponse) throws AuthLoginException {
        newDeviceRegistrationProfile.setDeviceName("Push Device");
        try {
            newDeviceRegistrationProfile.setCommunicationId(deviceResponse.get(COMMUNICATION_ID).asString());
            newDeviceRegistrationProfile.setDeviceMechanismUID(deviceResponse.get(MECHANISM_UID).asString());
            newDeviceRegistrationProfile.setCommunicationType(deviceResponse.get(COMMUNICATION_TYPE).asString());
            newDeviceRegistrationProfile.setDeviceType(deviceResponse.get(DEVICE_TYPE).asString());
            newDeviceRegistrationProfile.setDeviceId(deviceResponse.get(DEVICE_ID).asString());
        } catch (NullPointerException npe) {
            DEBUG.error("Blank value for necessary data from device response, {}", deviceResponse);
            throw failedAsLoginException();
        }

        try {
            recoveryCodes = recoveryCodeGenerator.generateCodes(NUM_RECOVERY_CODES, Alphabet.ALPHANUMERIC, false);
            newDeviceRegistrationProfile.setRecoveryCodes(recoveryCodes);
        } catch (CodeException e) {
            DEBUG.error("Insufficient recovery code generation occurred.");
            throw failedAsLoginException();
        }
        newDeviceRegistrationProfile.setIssuer(issuer);

        try {
            userPushDeviceProfileManager.saveDeviceProfile(
                    amIdentityPrincipal.getName(), realm, newDeviceRegistrationProfile);
        } catch (DevicePersistenceException e) {
            DEBUG.error("Unable to store device profile.");
            throw failedAsLoginException();
        }
    }

    private void paintRegisterDeviceCallback(AMIdentity id, String messageId, String challenge)
            throws AuthLoginException {
        replaceCallback(
                STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN,
                SCRIPT_OUTPUT_CALLBACK_INDEX,
                createQRCodeCallback(newDeviceRegistrationProfile, id, messageId, SCRIPT_OUTPUT_CALLBACK_INDEX,
                        challenge));
    }

    private Callback createQRCodeCallback(PushDeviceSettings deviceProfile, AMIdentity id, String messageId,
                                          int callbackIndex, String challenge) throws AuthLoginException {
        try {
            QRCallbackBuilder builder = new QRCallbackBuilder().withUriScheme("pushauth")
                    .withUriHost("push")
                    .withUriPath("forgerock")
                    .withUriPort(id.getName())
                    .withCallbackIndex(callbackIndex)
                    .addUriQueryComponent(LOADBALANCER_DATA_QR_CODE_KEY,
                            Base64url.encode((lbCookieValue).getBytes()))
                    .addUriQueryComponent(ISSUER_QR_CODE_KEY, Base64url.encode(issuer.getBytes()))
                    .addUriQueryComponent(MESSAGE_ID_QR_CODE_KEY, messageId)
                    .addUriQueryComponent(SHARED_SECRET_QR_CODE_KEY,
                            Base64url.encode(Base64.decode(deviceProfile.getSharedSecret())))
                    .addUriQueryComponent(BGCOLOUR_QR_CODE_KEY, bgColour)
                    .addUriQueryComponent(CHALLENGE_QR_CODE_KEY, Base64url.encode(Base64.decode(challenge)))
                    .addUriQueryComponent(REG_QR_CODE_KEY,
                            getMessageResponseUrl(pushService.getServiceAddressFor(realm,
                                    DefaultMessageTypes.REGISTER)))
                    .addUriQueryComponent(AUTH_QR_CODE_KEY,
                            getMessageResponseUrl(pushService.getServiceAddressFor(realm,
                                    DefaultMessageTypes.AUTHENTICATE)));

            if (imgUrl != null) {
                builder.addUriQueryComponent(IMG_QR_CODE_KEY, Base64url.encode(imgUrl.getBytes()));
            }

            return builder.build();
        } catch (PushNotificationException e) {
            DEBUG.error("Unable to read service addresses for Push Notification Service.");
            throw failedAsLoginException();
        }
    }

    private String getMessageResponseUrl(String component) {
        final BaseURLProvider baseUrlProvider = baseUrlProviderFactory.get(getRequestOrg());

        return Base64url.encode((baseUrlProvider.getRootURL(getHttpServletRequest()) + "/json" + component)
                .getBytes(StandardCharsets.UTF_8));
    }

    private AuthLoginException failedAsLoginException() throws AuthLoginException {
        setFailureID(amIdentityPrincipal.getName());
        throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
    }

    @Override
    public Principal getPrincipal() {
        return new AuthenticatorPushPrincipal(amIdentityPrincipal.getName());
    }

    private void setPollbackTimePeriod(long periodInMilliseconds) throws AuthLoginException {
        Callback[] callback = getCallback(STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN);
        PollingWaitCallback newPollingWaitCallback = PollingWaitCallback.makeCallback()
                .asCopyOf((PollingWaitCallback) callback[POLLING_TIME_OUTPUT_CALLBACK_INDEX])
                .withWaitTime(String.valueOf(periodInMilliseconds))
                .build();
        replaceCallback(STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN,
                POLLING_TIME_OUTPUT_CALLBACK_INDEX, newPollingWaitCallback);
    }

    private void setAppLinkCallbacks(int state) throws AuthLoginException {
        TextOutputCallback appleOutput = new TextOutputCallback(TextOutputCallback.INFORMATION, appleLink);
        TextOutputCallback googleOutput = new TextOutputCallback(TextOutputCallback.INFORMATION, googleLink);
        replaceCallback(state, APPLE_LINK_CALLBACK_INDEX, appleOutput);
        replaceCallback(state, GOOGLE_LINK_CALLBACK_INDEX, googleOutput);

    }
}
