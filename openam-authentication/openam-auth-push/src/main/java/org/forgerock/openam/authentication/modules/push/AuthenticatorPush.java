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
 * Copyright 2016-2018 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.push;

import static org.forgerock.openam.authentication.modules.push.Constants.AM_AUTH_AUTHENTICATOR_PUSH;
import static org.forgerock.openam.authentication.modules.push.Constants.CHALLENGE_KEY;
import static org.forgerock.openam.authentication.modules.push.Constants.DEVICE_PUSH_MESSAGE;
import static org.forgerock.openam.authentication.modules.push.Constants.DEVICE_PUSH_WAIT_TIMEOUT;
import static org.forgerock.openam.authentication.modules.push.Constants.EMERGENCY_CALLBACK_POSITION;
import static org.forgerock.openam.authentication.modules.push.Constants.EMERGENCY_NOT_PRESSED;
import static org.forgerock.openam.authentication.modules.push.Constants.EMERGENCY_PRESSED;
import static org.forgerock.openam.authentication.modules.push.Constants.LOADBALANCER_KEY;
import static org.forgerock.openam.authentication.modules.push.Constants.POLLING_CALLBACK_POSITION;
import static org.forgerock.openam.authentication.modules.push.Constants.RECOVERY_CODE_CALLBACK_POSITION;
import static org.forgerock.openam.authentication.modules.push.Constants.STATE_EMERGENCY;
import static org.forgerock.openam.authentication.modules.push.Constants.STATE_EMERGENCY_USED;
import static org.forgerock.openam.authentication.modules.push.Constants.STATE_WAIT;
import static org.forgerock.openam.authentication.modules.push.Constants.TIME_TO_LIVE_KEY;
import static org.forgerock.openam.authentication.modules.push.Constants.USERNAME_CALLBACK_LOCATION_POSITION;
import static org.forgerock.openam.authentication.modules.push.Constants.USERNAME_STATE;
import static org.forgerock.openam.authentication.modules.push.Constants.USE_EMERGENCY_CODE;
import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;

import com.amazonaws.services.sns.model.InvalidParameterException;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;

/**
 * ForgeRock Authentication (Push) Authentication Module.
 */
public class AuthenticatorPush extends AbstractPushModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthPush");

    //From config
    private Map<String, String> sharedState;

    //Internal state
    private String realm;
    private String username;
    private Principal principal;
    private String lbCookieValue;
    private long timeout;

    private PushDeviceSettings device;

    private PollingWaitAssistant pollingWaitAssistant;
    private long expireTime;

    private String pushMessage;

    // support for search with alias name
    private Set<String> userSearchAttributes = Collections.emptySet();

    private final AuthenticatorDeviceServiceFactory<AuthenticatorPushService> pushServiceFactory =
            InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<AuthenticatorDeviceServiceFactory<AuthenticatorPushService>>() { }));

    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        this.sharedState = sharedState;
        timeout = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));

        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());

        try {
            pushService.init(realm);
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: init() : Unable to init Push system.", e);
            return;
        }

        try {
            lbCookieValue = sessionCookies.getLBCookie(getSessionId());
        } catch (SessionException e) {
            DEBUG.warning("AuthenticatorPush :: init() : Unable to determine loadbalancer cookie value", e);
        }

        if (Boolean.parseBoolean(SystemPropertiesManager.get(nearInstantProperty))) {
            pollingWaitAssistant = new PollingWaitAssistant(timeout, 1000, 1000, 1000);
        } else {
            pollingWaitAssistant = new PollingWaitAssistant(timeout);
        }

        pushMessage = CollectionHelper.getMapAttr(options, DEVICE_PUSH_MESSAGE);

        try {
            userSearchAttributes = getUserAliasList();
        } catch (final AuthLoginException ale) {
            DEBUG.warning("AuthenticatorPush :: init() : Unable to retrieve search attributes", ale);
        }
    }

    @Override
    public int process(final Callback[] callbacks, int state) throws LoginException {

        final HttpServletRequest request = getHttpServletRequest();

        if (request == null) {
            DEBUG.error("AuthenticatorPush :: process() : Request was null.");
            throw failedAsLoginException();
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return loginStart();
        case USERNAME_STATE:
            return usernameState(callbacks);
        case STATE_WAIT:
            return stateWait(callbacks);
        case STATE_EMERGENCY:
            return emergencyState(callbacks, username, realm);
        case STATE_EMERGENCY_USED:
            return completeLogin();
        default:
            DEBUG.error("AuthenticatorPush :: process() : Invalid state.");
            throw failedAsLoginException();
        }
    }

    private int completeLogin() throws AuthLoginException {
        storeUsername(username);
        setCurrentPrincipal();
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private int stateWait(Callback[] callbacks) throws AuthLoginException {
        checkDeviceExists();
        if (expireTime < Time.currentTimeMillis()) {
            throw failedAsPasswordException();
        }
        if (emergencyPressed(callbacks)) {
            return STATE_EMERGENCY;
        } else {
            return pollForResponse();
        }
    }

    private boolean emergencyPressed(Callback[] callbacks) {
        ConfirmationCallback callback = (ConfirmationCallback) callbacks[EMERGENCY_CALLBACK_POSITION];
        return callback.getSelectedIndex() == EMERGENCY_PRESSED;
    }

    private void checkDeviceExists() throws AuthLoginException {
        if (device == null) {
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

    private int emergencyState(Callback[] callbacks, String username, String realm) throws AuthLoginException {

        NameCallback recoveryCode = (NameCallback) callbacks[RECOVERY_CODE_CALLBACK_POSITION];
        String codeAttempt = recoveryCode.getName();

        if (device.useRecoveryCode(codeAttempt)) {
            try {
                userPushDeviceProfileManager.saveDeviceProfile(username, realm, device);
            } catch (DevicePersistenceException e) {
                throw failedAsLoginException();
            }

            return STATE_EMERGENCY_USED;
        }

        throw failedAsPasswordException();
    }

    private int pollForResponse() throws AuthLoginException {

        switch (pollingWaitAssistant.getPollingWaitState()) {
        case TOO_EARLY:
            setEmergencyButton();
            return STATE_WAIT;
        case NOT_STARTED:
        case WAITING:
            return waitingChecks();
        case COMPLETE:
            return completeChecks();
        case TIMEOUT:
            DEBUG.warning("AuthenticatorPush :: timeout value exceeded while waiting for response.");
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        case SPAMMED:
            DEBUG.warning("AuthenticatorPush :: too many requests sent to Auth module.  "
                    + "Client should obey wait time.");
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

    private int completeChecks() throws AuthLoginException {
        try {
            MessageState state = messagePromise.getMessageState();
            if (state != MessageState.UNKNOWN) {
                return determineSuccess(state);
            }
        } catch (PushNotificationException | CoreTokenException e) {
            DEBUG.error("Unable to locate Push Service.", e);
        }

        throw failedAsLoginException();
    }

    private int waitingChecks() throws AuthLoginException {
        try {
            MessageState state = pushService.getMessageHandlers(realm).get(DefaultMessageTypes.AUTHENTICATE)
                 .check(messagePromise.getMessageId());
            if (state != MessageState.UNKNOWN) {
                return determineSuccess(state);
            }
        } catch (CoreTokenException e) {
            DEBUG.warning("CTS threw exception, falling back to local MessageDispatcher.", e);
        } catch (PushNotificationException e) {
            DEBUG.error("Unable to locate Push Service.", e);
            throw failedAsLoginException();
        }

        setPollbackTimePeriod(pollingWaitAssistant.getWaitPeriod());
        pollingWaitAssistant.resetWait();
        setEmergencyButton();
        return STATE_WAIT;
    }

    private int determineSuccess(MessageState state)
            throws AuthLoginException, PushNotificationException, CoreTokenException {
        pushService.getMessageHandlers(realm).get(DefaultMessageTypes.AUTHENTICATE)
                .delete(messagePromise.getMessageId());
        if (state == MessageState.DENIED) {
            throw failedAsPasswordException();
        } else {
            return completeLogin();
        }
    }

    private int loginStart() throws AuthLoginException {
        try {
            if (isSessionUpgrade()) {
                username = getUsernameFromOldSession();
            } else if (username == null && sharedState != null) {
                username = sharedState.get(getUserKey());
            }
        } catch (AuthLoginException | SSOException e) {
            throw failedAsLoginException();
        }

        if (username == null) {
            extractSubjectDataFromExistingSession();
        }

        if (username == null) {
            return USERNAME_STATE;
        } else {
            AMIdentity id = IdUtils.getIdentity(username, realm, userSearchAttributes);
            if (id == null) {
                throw failedAsLoginException();
            }

            try {
                realmService = pushServiceFactory.create(realm);
            } catch (SMSException | SSOException e) {
                DEBUG.error("{} :: init() : Unable to retrieve the Push Service Factory.", AM_AUTH_AUTHENTICATOR_PUSH,
                        e);
                throw failedAsLoginException();
            }

            isSkippable = isOptional(realmService);

            try {
                if (isSkippable) {
                    userConfiguredSkippable = userConfiguredSkippable(id, realmService);
                }
            } catch (IdRepoException | SSOException e) {
                DEBUG.warning("AuthenticatorPush :: sendState() : Failed to detect skippable necessity.");
                throw failedAsLoginException();
            }

            if (userConfiguredSkippable == SkipSetting.SKIPPABLE && isSkippable) {
                return completeLogin();
            }

            device = getDevice(id.getName(), realm);
            if (sendMessage(device)) {
                this.expireTime = Time.currentTimeMillis() + timeout;
                setEmergencyButton();
                return STATE_WAIT;
            } else {
                DEBUG.warning("AuthenticatorPush :: loginStart() : Failed to send message.");
                throw failedAsLoginException();
            }
        }
    }

    private int usernameState(Callback[] callbacks) throws AuthLoginException {
        Reject.ifNull(callbacks);
        NameCallback nameCallback = (NameCallback) callbacks[USERNAME_CALLBACK_LOCATION_POSITION];
        username = nameCallback.getName();

        if (StringUtils.isBlank(username)) {
            DEBUG.warning("AuthenticatorPush :: usernameState() : Username was blank.");
            throw failedAsLoginException();
        }

        return ISAuthConstants.LOGIN_START;
    }

    void setCurrentPrincipal() throws AuthLoginException {

        AMIdentity id = IdUtils.getIdentity(username, realm, userSearchAttributes);

        try {
            if (id != null && id.isExists() && id.isActive()) {
                principal = new AuthenticatorPushPrincipal(username);
            }
        } catch (IdRepoException | SSOException e) {
            DEBUG.warning("AuthenticatorPush :: Failed to locate user {} ", username, e);
            throw failedAsLoginException();
        }
    }

    private boolean sendMessage(PushDeviceSettings device) {

        String communicationId = device.getCommunicationId();
        String mechanismId = device.getDeviceMechanismUID();

        String challenge = userPushDeviceProfileManager.createRandomBytes();

        JwtClaimsSetBuilder jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim(Constants.MECHANISM_ID_KEY, mechanismId)
                .claim(LOADBALANCER_KEY, Base64.encode((lbCookieValue).getBytes()))
                .claim(CHALLENGE_KEY, challenge)
                .claim(TIME_TO_LIVE_KEY, String.valueOf(timeout / 1000));

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newHmacSigningHandler(Base64.decode(device.getSharedSecret())))
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.HS256).done().build();

        pushMessage = pushMessage.replaceAll("\\{\\{user\\}\\}", username);
        pushMessage = pushMessage.replaceAll("\\{\\{issuer\\}\\}", device.getIssuer());

        MessageId messageId = messageIdFactory.create(DefaultMessageTypes.AUTHENTICATE);
        PushMessage message = new PushMessage(communicationId, jwt, pushMessage, messageId);

        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(
                new SignedJwtVerificationPredicate(Base64.decode(device.getSharedSecret()), JWT));
        servicePredicates.add(
                new PushMessageChallengeResponsePredicate(Base64.decode(device.getSharedSecret()), challenge, JWT));

        try {
            Set<Predicate> predicates = pushService.getMessagePredicatesFor(realm)
                    .get(DefaultMessageTypes.AUTHENTICATE);
            if (predicates != null) {
                servicePredicates.addAll(predicates);
            }
            messagePromise = pushService.getMessageDispatcher(realm).expectLocally(messageId, servicePredicates);
            pushService.send(message, realm);
            pollingWaitAssistant.start(messagePromise.getPromise());
        } catch (NotFoundException | PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: sendMessage() : Failed to transmit message through PushService.");
            return false;
        } catch (CoreTokenException e) {
            DEBUG.warning("Unable to persist message token in core token service.", e);
            return false;
        }

        return true;
    }

    private void extractSubjectDataFromExistingSession() throws AuthLoginException {
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            Session sess = getLoginState("Push").getOldSession();
            if (sess == null) {
                return; // leave username as null
            }
            SSOToken token = mgr.createSSOToken(sess.getID().toString());
            username = token.getProperty("UserToken");
        } catch (SSOException sso) {
            // leave username as null, no error
        }

    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    private InvalidPasswordException failedAsPasswordException() throws InvalidParameterException {
        setFailureID(username);
        return new InvalidPasswordException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
    }

    private AuthLoginException failedAsLoginException() throws AuthLoginException {
        setFailureID(username);
        return new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
    }

    private void setPollbackTimePeriod(long periodInMilliseconds) throws AuthLoginException {
        PollingWaitCallback newPollingWaitCallback = PollingWaitCallback.makeCallback()
                .asCopyOf((PollingWaitCallback) getCallback(STATE_WAIT)[POLLING_CALLBACK_POSITION])
                .withWaitTime(String.valueOf(periodInMilliseconds))
                .build();
        replaceCallback(STATE_WAIT, POLLING_CALLBACK_POSITION, newPollingWaitCallback);
    }

    private void setEmergencyButton() throws AuthLoginException {
        ConfirmationCallback confirmationCallback =
                new ConfirmationCallback(ConfirmationCallback.INFORMATION, USE_EMERGENCY_CODE, EMERGENCY_PRESSED);
        confirmationCallback.setSelectedIndex(EMERGENCY_NOT_PRESSED);
        replaceCallback(STATE_WAIT, EMERGENCY_CALLBACK_POSITION, confirmationCallback);
    }
}
