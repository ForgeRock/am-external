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
package org.forgerock.openam.services.push;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.openam.secrets.rotation.SecretLabelListener;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.MessageDispatcherFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class PushNotificationServiceTest {

    private PushNotificationServiceConfigHelperFactory mockHelperFactory;
    private PushNotificationServiceConfigHelper mockHelper;
    private PushNotificationServiceConfig.Realm realmConfig;
    private PushNotificationDelegateFactory mockDelegateFactory;
    private PushNotificationDelegate mockDelegate;
    private PushNotificationDelegate mockOldDelegate;
    private MessageDispatcherFactory mockMessageDispatcherFactory;
    private PushNotificationServiceConfig config;
    private MessageId mockMessageId;
    private static PushNotificationDelegate mockTestDelegate;

    private SecretLabelListener secretLabelListener;

    private PushNotificationService notificationService;

    @BeforeMethod
    public void theSetUp() throws SMSException, SSOException {
        this.mockHelperFactory = mock(PushNotificationServiceConfigHelperFactory.class);
        this.mockHelper = mock(PushNotificationServiceConfigHelper.class);
        this.realmConfig = new PushNotificationServiceConfig.Realm() {
            @Override
            public String accessKey() {
                return "accessKey";
            }

            @Override
            public String appleEndpoint() {
                return "appleEndpoint";
            }

            @Override
            public String googleEndpoint() {
                return "googleEndpoint";
            }

            @Override
            public char[] secret() {
                return "secret".toCharArray();
            }

            @Override
            public String delegateFactory() {
                return "factoryClass";
            }

            @Override
            public String region() {
                return "us-west-2";
            }
        };
        this.mockDelegateFactory = mock(PushNotificationDelegateFactory.class);
        this.mockDelegate = mock(PushNotificationDelegate.class);
        this.mockOldDelegate = mock(PushNotificationDelegate.class);
        this.mockMessageDispatcherFactory = mock(MessageDispatcherFactory.class);
        this.config = mock(PushNotificationServiceConfig.class);
        this.secretLabelListener = mock(SecretLabelListener.class);
        mockMessageId = mock(MessageId.class);
        mockTestDelegate = mock(PushNotificationDelegate.class);

        ConcurrentMap<String, PushNotificationDelegate> pushRealmMap = new ConcurrentHashMap<>();
        pushRealmMap.put("realm", mockDelegate);
        pushRealmMap.put("oldRealm", mockOldDelegate);
        ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap = new ConcurrentHashMap<>();
        pushFactoryMap.put("factoryClass", mockDelegateFactory);

        given(mockHelper.getConfig()).willReturn(realmConfig);
        given(mockHelperFactory.getConfigHelperFor("realm2")).willReturn(mockHelper);
        given(mockHelperFactory.getConfigHelperFor("realm4")).willThrow(new SMSException("Error reading service"));

        this.notificationService = new PushNotificationService(pushRealmMap, pushFactoryMap,
                mockHelperFactory, mockMessageDispatcherFactory, config, secretLabelListener);
    }

    @Test
    public void shouldSendMessage() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);

        //when
        notificationService.send(pushMessage, "realm");

        //then
        verify(mockDelegate, times(1)).send(pushMessage);
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldErrorIfRealmNotInitiated() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestDelegateFactory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    @Test
    public void shouldLoadDelegateAndSendMessage() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestDelegateFactory");

        //when
        notificationService.init("realm2");
        notificationService.send(pushMessage, "realm2");

        //then
        verify(mockTestDelegate, times(1)).startServices();
        verify(mockTestDelegate, times(1)).send(pushMessage);
        verifyNoMoreInteractions(mockTestDelegate);
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenDelegateCannotLoad() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);
        given(mockHelper.getFactoryClass()).willReturn("invalid factory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenConfigNotFound() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);

        //when
        notificationService.send(pushMessage, "realm4");

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenDelegateFactoryIsBroken() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", "message", "subject", mockMessageId);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestBrokenDelegateFactory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    /**
     * TEST IMPLEMENTATIONS.
     */

    public static class TestDelegateFactory implements PushNotificationDelegateFactory {
        @Override
        public PushNotificationDelegate produceDelegateFor(PushNotificationServiceConfig.Realm config, String realm,
                                               MessageDispatcher messageDispatcher) throws PushNotificationException {
            return mockTestDelegate;
        }
    }

    public static class TestBrokenDelegateFactory implements PushNotificationDelegateFactory {
        @Override
        public PushNotificationDelegate produceDelegateFor(PushNotificationServiceConfig.Realm config, String realm,
                                               MessageDispatcher messageDispatcher) throws PushNotificationException {
            throw new PushNotificationException("Broken implementation.");
        }
    }

    /**
     * INNER CLASS.
     */

    @Test
    public void shouldKeepExistingDelegate() throws PushNotificationException {
        //given
        given(mockOldDelegate.isRequireNewDelegate(realmConfig)).willReturn(false);

        //when
        notificationService.new PushNotificationDelegateUpdater().replaceDelegate("oldRealm", mockDelegate,
                realmConfig);

        //then
        verify(mockOldDelegate, times(1)).isRequireNewDelegate(realmConfig);
        verify(mockOldDelegate, times(1)).updateDelegate(realmConfig);
        verifyNoMoreInteractions(mockOldDelegate);
    }

    @Test
    public void shouldCloseAndReplaceOldDelegate() throws PushNotificationException, IOException {
        //given
        given(mockOldDelegate.isRequireNewDelegate(realmConfig)).willReturn(true);

        //when
        notificationService.new PushNotificationDelegateUpdater().replaceDelegate("oldRealm", mockDelegate,
                realmConfig);

        //then
        verify(mockOldDelegate, times(1)).isRequireNewDelegate(realmConfig);
        verify(mockOldDelegate, times(1)).close();
        verify(mockDelegate, times(1)).startServices();
        verifyNoMoreInteractions(mockOldDelegate);
    }

}
