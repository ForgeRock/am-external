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
package com.sun.identity.plugin.session.impl;

import static org.forgerock.openam.guice.FederationGuiceModule.*;

import java.net.InetAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.continuous.watching.ContinuousListener;
import org.forgerock.util.Reject;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.watchers.listeners.SessionDeletionListener;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.saml2.idpdiscovery.Debug;

/**
 * Responsible for providing notifications for session objects as part of OpenAM Federation implementation.
 *
 * <p>Callers of this service can register a listener against individual sessions. Registered sessions will
 * then be tracked and when they are known to have timed out, the caller will be notified via
 * {@link SessionListener}.
 *
 * <p>This class will use two ways of keeping the caller up to date on the state of the
 * session.
 *
 * <ul>
 *     <li><b>Notifications</b>: Provided by {@link SessionService#registerListener(ContinuousListener)} if
 *     they are supported on the current session. Allows notification when the session is destroyed.
 *
 *     <li><b>Session Timeout</b>: In the event that the {@link ContinuousListener} functionality is not
 *     available, or is temporarily unavailable, this service will indicate when the session has timed out
 *     based on information held within the session.
 * </ul>
 *
 */
public class FMSessionNotification {
    private final Debug debug = Debug.getInstance(FMSessionNotification.class.getName());

    private final SessionService sessionService;
    private final ScheduledExecutorService scheduledService;

    private final ConcurrentMap<String, Registration> store = new ConcurrentHashMap<>();

    private final SessionDeletionListener sessionDeletionListener = new SessionDeletionListener() {
        @Override
        public void sessionDeleted(String sessionId) {
            notifyListeners(sessionId);
        }

        @Override
        public void connectionLost() {
        }

        @Override
        public void initiationFailed() {
        }

        @Override
        public void connectionReestablished() {

        }
    };

    @Inject
    public FMSessionNotification(@Named(FEDERATION_SESSION_MANAGEMENT) ScheduledExecutorService scheduledService,
                                 SessionService sessionService) {
        this.scheduledService = scheduledService;
        this.sessionService = sessionService;
    }

    /**
     * Registers the provided {@link SSOToken} for notification of changes on that session.
     *
     * <p>If the session changes state by being logged out or timed out, then the provided
     * {@link SessionListener} will be notified once about this change.
     *
     * <p>Multiple listeners are allowed for the same session, each listener added will be
     * notified once when the session changes state.
     *
     * @param token Non null SSOToken which must exist as a session within OpenAM
     * @param listener Non null listener instance to be notified on state change
     *
     * @throws SessionException If there was an unexpected error establishing notifications for the listener.
     */
    public void store(SSOToken token, SessionListener listener) throws SessionException {
        Reject.ifNull(token);
        Reject.ifNull(listener);

        final String tokenId = token.getTokenID().toString();

        Registration reg = new Registration(token);
        Registration oldReg = store.putIfAbsent(tokenId, reg);
        if (null != oldReg) {
            reg = oldReg;
        }

        // Associate the listener with the session
        reg.addListener(listener);

        if (reg.getFuture() == null) {
            try {
                // Associate the scheduled future with the session
                reg.setFuture(scheduledService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        notifyListeners(tokenId);
                    }
                }, getTimeoutInMinutes(token), TimeUnit.MINUTES));
            } catch (SSOException e) { // Only thrown if we fail to get timeout on session - not expected.
                throw new SessionException(e);
            }

            // Setup notifications for the session.
            try {
                sessionService.notifyListenerFor(new SessionID(tokenId), sessionDeletionListener);
            } catch (com.iplanet.dpro.session.SessionException e) {
                debug.warning("Error whilst registering session for notifications", e);
            }
        }
    }

    /**
     * Notify all registered listeners that the session has been removed.
     *
     * <p>Perform all required cleanup, including canceling the future timeout on the session.
     *
     * @param sessionID The key which the {@link Registration} was stored under.
     */
    private void notifyListeners(String sessionID) {
        Registration reg = store.remove(sessionID);
        if (reg == null) {
            return;
        }

        for (SessionListener listener : reg.getListeners()) {
            listener.sessionInvalidated(reg.getToken());
        }

        ScheduledFuture<?> future = reg.getFuture();
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Select an appropriate time at which we know the Session is timed out.
     *
     * Current implementation is based purely on the maximum session time which is known
     * to vary between sessions.
     *
     * @param token Non null token used for timeout calculation.
     * @return A delay of zero or more minutes from now when the session will timeout.
     * @throws SSOException If there was an unexpected error retrieving session timeout.
     */
    private long getTimeoutInMinutes(SSOToken token) throws SSOException { // checked exception is a concession to SSO API
        long timeout = token.getMaxSessionTime();
        if (timeout < 0) {
            timeout = 0;
        }
        return timeout;
    }

    /**
     * Registration binds the SSOToken provided with information required by this implementation.
     *
     * <b>Future</b>: Is stored once the timeout task has been added to the scheduled service. This
     * acts as a handle to cancel the task in the event that the notification arrives before the
     * session timeouts.
     *
     * <b>Listeners</b>: There can be many listeners for each SSOToken which need to be notified only once
     * when the session changes state.
     */
    private class Registration {
        private ScheduledFuture<?> future;

        private SSOToken session;
        private final Set<SessionListener> listeners = new HashSet<>();

        /**
         * @param session Non null {@link SSOToken}
         */
        private Registration(SSOToken session) {
            this.session = new StoredTokenProperties(session);
        }

        /**
         * @return Handle allowing cancellation of the timeout task.
         */
        private ScheduledFuture<?> getFuture() {
            return future;
        }

        /**
         * @param future Non null future for future use.
         */
        private void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        private Set<SessionListener> getListeners() {
            return Collections.unmodifiableSet(listeners);
        }

        /**
         * @param listener Listener to add to this {@link Registration}.
         */
        private void addListener(SessionListener listener) {
            listeners.add(listener);
        }

        /**
         * @return The {@link SSOToken} that was originally provided.
         */
        private SSOToken getToken() {
            return session;
        }
    }

    /**
     * Saves the Token Properties for use on token deletion notification.  The New session model means that tokens
     * that have been deleted cannot have their properties queried.  By keeping the properties in here we can
     * use them later without having to change code to takes a hashmap.
     */
    private static class StoredTokenProperties implements SSOToken {

        SSOTokenID tokenId = null;
        final Map<String, String> copyProps = new HashMap<>();

        StoredTokenProperties(SSOToken session) {
            try {
                this.tokenId = session.getTokenID();
                this.copyProps.putAll(session.getProperties());
            } catch (SSOException e) {
            }
        }

        @Override
        public Principal getPrincipal() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthType() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getAuthLevel() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InetAddress getIPAddress() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHostName() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getTimeLeft() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getMaxSessionTime() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getIdleTime() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getMaxIdleTime() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SSOTokenID getTokenID() {
            return tokenId;
        }

        @Override
        public void setProperty(String name, String value) throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getProperty(String name) throws SSOException {
            return copyProps.get(name);
        }

        @Override
        public String getProperty(String name, boolean ignoreState) throws SSOException {
            return getProperty(name);
        }

        @Override
        public Map<String, String> getProperties() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addSSOTokenListener(SSOTokenListener listener) throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isTokenRestricted() throws SSOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String dereferenceRestrictedTokenID(SSOToken requester, String restrictedId) throws SSOException {
            throw new UnsupportedOperationException();
        }
    }
}
