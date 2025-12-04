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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.services.push;

import static org.forgerock.am.config.Listener.ServiceListenerEvent;
import static org.forgerock.openam.services.push.PushNotificationConstants.SERVICE_NAME;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.rotation.ReloadableSecret;
import org.forgerock.openam.secrets.rotation.SecretLabelListener;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.MessageDispatcherFactory;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.shared.secrets.Labels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * The PushNotificationManager holds a map of instantiated PushNotificationDelegates to their realm in which they exist.
 * The PushNotificationManager produces PushNotificationDelegates in accordance with the PushNotificationDelegate
 * interface, using a provided PushNotificationDelegateFactory per realm's instantiated service.
 *
 * PushNotificationDelegateFactories are stored in a cache, so as to only load each factory class once.
 *
 * Creating a service config will not update the PushNotificationService unless it has been previously instantiated
 * by another component in the system. Therefore each time the service is called upon to perform a task such as
 * sending a message it checks that there exists a delegate to handle that request.
 *
 * If no delegate has been configured, the service will attempt to load the config for that realm before accessing
 * the delegate instance. Updating the service config via the service interface (after the service has been
 * instantiated) also causes the attempt to load the config & mint a delegate.
 *
 * Later changes in the config will update the delegateFactory and depending upon the delegate's implementation of
 * the isRequireNewDelegate(PushNotificationConfig) method may require the generation of a new delegate. If
 * no new delegate is required, it may be appropriate to update the existing delegate's (non-connection-relevant)
 * configuration parameters. It may also be appropriate to leave the delegate exactly as it was if no configuration
 * option has been altered -- there's no point in tearing down the listeners and services in the case we're
 * re-creating the same delegate.
 */
@Singleton
public class PushNotificationService implements ReloadableSecret {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    /**
     * Holds a map of the current PushNotificationDelegate for a given realm.
     */
    private final ConcurrentMap<String, PushNotificationDelegate> pushRealmMap;

    /**
     * Holds a cache of all pushNotificationDelegateFactories we have used for quick loading.
     */
    private final ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap;

    private final Logger debug = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushNotificationServiceConfigHelperFactory configHelperFactory;

    private final PushNotificationDelegateUpdater delegateUpdater;

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final PushNotificationServiceConfig config;

    private final SecretLabelListener secretLabelListener;

    /**
     * Constructor (called by Guice), registers a listener for this class against all PushNotificationService changes in
     * a realm.
     *
     * @param pushRealmMap             Map holding all delegates mapped to the realm in which they belong.
     * @param pushFactoryMap           Map holding all factories registered during the lifetime of this service.
     * @param configHelperFactory      Produces config helpers for the appropriate realms.
     * @param messageDispatcherFactory Produces MessageDispatchers according to the configured options.
     * @param config                   The Push Notification config.
     * @param secretLabelListener      The guice managed secret label listener instance.
     */
    @Inject
    public PushNotificationService(ConcurrentMap<String, PushNotificationDelegate> pushRealmMap,
                                   ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap,
                                   PushNotificationServiceConfigHelperFactory configHelperFactory,
                                   MessageDispatcherFactory messageDispatcherFactory,
                                   PushNotificationServiceConfig config, SecretLabelListener secretLabelListener) {
        this.pushRealmMap = new ConcurrentHashMap<>(pushRealmMap);
        this.pushFactoryMap = new ConcurrentHashMap<>(pushFactoryMap);
        this.secretLabelListener = secretLabelListener;
        this.delegateUpdater = new PushNotificationDelegateUpdater();
        this.configHelperFactory = configHelperFactory;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.config = config;
    }

    /**
     * Registers the service listener to ensure that updates are caught and responded to.
     */
    public void registerServiceListener() {
        config.watch()
                .onRealmChange(realm -> {
                    synchronized (pushRealmMap) {
                        updatePreferencesCatchingExceptions(realm.asPath());
                    }
                }, ServiceListenerEvent.ADDED, ServiceListenerEvent.MODIFIED)
                .onRealmChange(realm -> {
                    try {
                        deleteService(realm.asPath());
                    } catch (PushNotificationException e) {
                        debug.error("Unable to update preferences for organization {}", realm.asPath(), e);
                    }
                }, ServiceListenerEvent.REMOVED)
                    .listen();
    }

    /**
     * Callback triggered if the secret label mapping is changed.
     *
     * @param realm The realm the secret was changed in.  This will be 'null' if it was a global secrets change.
     */
    @Override
    public void setNeedsReload(Realm realm) {
        synchronized (pushRealmMap) {
            if (realm == null) { // Global secret mapping has changed, so update all delegates.
                for (String realmPath : pushRealmMap.keySet()) {
                    updatePreferencesCatchingExceptions(realmPath);
                }
            } else {
                String realmPath = realm.asPath();
                if (pushRealmMap.containsKey(realmPath)) {
                    updatePreferencesCatchingExceptions(realmPath);
                }
            }
        }
    }

    @Override
    public String getLabel() {
        return Labels.PUSH_NOTIFICATION_PASSWORD;
    }

    /**
     * Register this class as a listener for any secret mapping changes.
     */
    public void registerSecretListeners() {
        secretLabelListener.addListener(this);
    }

    /**
     * Primary method of this class. Used to communicate via the appropriate delegate for this realm out
     * to a Push communication service such as SNS.
     *
     * @param message the message to transmit.
     * @param realm the realm from which to transmit the message.
     * @throws PushNotificationException if there are problems initialising the service or sending the notification.
     */
    public void send(PushMessage message, String realm) throws PushNotificationException {

        PushNotificationDelegate delegate = getDelegateForRealm(realm);

        if (delegate == null) {
            throw new PushNotificationException("No delegate for supplied realm. Check service exists and init has "
                    + "been called.");
        }

        delegate.send(message);
    }

    /**
     * Initializes the PushNotification system for this realm. If the system is already up-to-date and
     * operational in the realm, this call will make no changes.
     *
     * @param realm Realm in which this PushNotification system exists.
     * @throws PushNotificationException If there were issues configuring the Push system.
     */
    public void init(String realm) throws PushNotificationException {
        if (!pushRealmMap.containsKey(realm)) {
            synchronized (pushRealmMap) { //wait here for the thread with first access to update
                if (!pushRealmMap.containsKey(realm)) {
                    try {
                        updatePreferences(realm);
                    } catch (NoSuchElementException e) {
                        debug.warn("No Push Notification Service Config found for realm " + realm, e);
                    }
                    if (!pushRealmMap.containsKey(realm)) {
                        debug.warn("No Push Notification Delegate configured for realm {}", realm);
                        throw new PushNotificationException("No Push Notification Delegate configured for this realm.");
                    }
                }
            }
        }
    }

    /**
     * Retrieve the MessageDispatcher message return-route object for this realm's delegate.
     *
     * @param realm The realm whose delegate's MessageDispatcher to return.
     * @return The MessageDispatcher belonging to the delegate associated with the realm requested.
     * @throws NotFoundException If there was no delegate configured for the given realm.
     */
    public MessageDispatcher getMessageDispatcher(String realm) throws NotFoundException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getMessageDispatcher();
        }

        throw new NotFoundException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Returns the relative location of the service endpoint for the provided {@link MessageType} in this realm.
     *
     * @param realm The realm of the service to check.
     * @param messageType The message type this endpoint location should handle.
     * @return The relative location of the service.
     * @throws PushNotificationException if the is not service available for that realm.
     */
    public String getServiceAddressFor(String realm, MessageType messageType) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getMessageTypeEndpoint(messageType);
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Get the current delegate for the provided realm.
     *
     * @param realm Realm whose delegate you wish to retrieve.
     * @return The current mapping for that realm, or null if one does not exist.
     */
    private PushNotificationDelegate getDelegateForRealm(String realm) {
        return pushRealmMap.get(realm);
    }

    /**
     * Gets the message {@link Predicate}s for various {@link DefaultMessageTypes} for this delegate implementation.
     *
     * @param realm The realm of the service to check.
     * @return A map of message types to sets of predicates.
     * @throws PushNotificationException if the is not service available for that realm.
     */
    public Map<MessageType, Set<Predicate>> getMessagePredicatesFor(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getMessagePredicates();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Gets the {@link ClusterMessageHandler}s for various {@link DefaultMessageTypes} for this delegate implementation.
     *
     * @param realm The realm of the service to check.
     * @return A map of message types to sets of predicates.
     * @throws PushNotificationException if the is not service available for that realm.
     */
    public Map<MessageType, ClusterMessageHandler> getMessageHandlers(String realm) throws PushNotificationException {
        if (pushRealmMap.containsKey(realm)) {
            return pushRealmMap.get(realm).getMessageHandlers();
        }

        throw new PushNotificationException("No Push Notification Service available for realm " + realm);
    }

    /**
     * Gets the {@link PushNotificationDelegate} for the provided realm.
     *
     * @param realm The realm to get the delegate for.
     * @return The delegate for the realm.
     * @throws PushNotificationException If there was an issue getting the delegate.
     */
    public PushNotificationDelegate getDelegate(String realm) throws PushNotificationException {
        PushNotificationServiceConfigHelper configHelper = getConfigHelper(realm);
        String factoryClass = configHelper.getFactoryClass();
        validateFactoryExists(factoryClass);
        return pushFactoryMap.get(factoryClass)
                .produceDelegateFor(configHelper.getConfig(), realm, createDispatcher(configHelper));
    }

    private void updatePreferences(String realm) throws PushNotificationException {
        var config = getConfigHelper(realm).getConfig();
        PushNotificationDelegate pushNotificationDelegate = getDelegate(realm);
        if (pushNotificationDelegate == null) {
            debug.error("PushNotificationFactory produced a null delegate. Aborting update.");
        }

        delegateUpdater.replaceDelegate(realm, pushNotificationDelegate, config);
        init(realm);
    }

    private MessageDispatcher createDispatcher(PushNotificationServiceConfigHelper configHelper) {
        long maxSize = configHelper.getConfig().mdCacheSize();
        int concurrency = configHelper.getConfig().mdConcurrency();
        int duration = configHelper.getConfig().mdDuration();

        return messageDispatcherFactory.create(maxSize, concurrency, duration);
    }

    private void deleteService(String realm) throws PushNotificationException {
        delegateUpdater.deleteDelegate(realm);
    }

    private void validateFactoryExists(String factoryClass) throws PushNotificationException {
        try {
            pushFactoryMap.putIfAbsent(factoryClass, createFactory(factoryClass));
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            debug.error("Unable to instantiate PushNotificationDelegateFactory class.", e);
            throw new PushNotificationException("Unable to instantiate PushNotificationDelegateFactory class.", e);
        }
    }

    private PushNotificationServiceConfigHelper getConfigHelper(String realm) throws PushNotificationException {
        try {
            return configHelperFactory.getConfigHelperFor(realm);
        } catch (SSOException | SMSException e) {
            debug.warn("Unable to read config for PushNotificationConfig in realm {}", realm);
            throw new PushNotificationException("Unable to find config for PushNotificationConfig.", e);
        }
    }

    private PushNotificationDelegateFactory createFactory(String factoryClass)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (PushNotificationDelegateFactory) Class.forName(factoryClass).newInstance();
    }

    private void updatePreferencesCatchingExceptions(String realmPath) {
        try {
            updatePreferences(realmPath);
        } catch (PushNotificationException e) {
            debug.error("Unable to update preferences for organization {}", realmPath, e);
        } catch (NoSuchElementException e) {
            debug.warn("No Push Notification Service Config found for realm " + realmPath, e);
        }
    }

    /**
     * Our delegate updater.
     */
    @VisibleForTesting
    final class PushNotificationDelegateUpdater {
        void replaceDelegate(String realm, PushNotificationDelegate newDelegate,
                         PushNotificationServiceConfig.Realm config) throws PushNotificationException {
            try {
                PushNotificationDelegate oldDelegate = pushRealmMap.get(realm);

                if (oldDelegate == null) {
                    start(realm, newDelegate);
                } else {
                    if (oldDelegate.isRequireNewDelegate(config)) {
                        pushRealmMap.remove(realm);
                        oldDelegate.close();
                        start(realm, newDelegate);
                    } else {
                        oldDelegate.updateDelegate(config);
                    }
                }
            } catch (IOException e) {
                debug.error("Unable to call close on the old delegate having removed it from the realmPushMap.", e);
                throw new PushNotificationException("Error calling close on the previous delegate instance.", e);
            }
        }

        private void start(String realm, PushNotificationDelegate delegate) throws PushNotificationException {
            delegate.startServices();
            pushRealmMap.put(realm, delegate);
        }

        private void deleteDelegate(String realm) throws PushNotificationException {
            PushNotificationDelegate removedDelegate = pushRealmMap.remove(realm);
            try {
                removedDelegate.close();
            } catch (IOException e) {
                throw new PushNotificationException("Error Deleting Service " + SERVICE_NAME + " for realm: " + realm);
            }
        }
    }

}
