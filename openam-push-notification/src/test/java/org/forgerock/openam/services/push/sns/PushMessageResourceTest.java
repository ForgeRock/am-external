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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openam.services.push.sns;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageType;
import org.forgerock.openam.services.push.PushMessageResource;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.PredicateNotMetException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class PushMessageResourceTest {

    PushMessageResource messageResource;
    PushNotificationService mockService;
    MessageDispatcher mockDispatcher;
    RealmTestHelper realmTestHelper;
    ClusterMessageHandler mockMessageHandler;
    HashMap<MessageType, ClusterMessageHandler> messageTypeClusterMap;

    @BeforeMethod
    public void theSetUp() throws Exception { //you need this

        mockService = mock(PushNotificationService.class);
        Debug mockDebug = mock(Debug.class);
        mockDispatcher = mock(MessageDispatcher.class);
        mockMessageHandler = mock(ClusterMessageHandler.class);
        messageTypeClusterMap = new HashMap<>();
        messageTypeClusterMap.put(DefaultMessageTypes.AUTHENTICATE, mockMessageHandler);
        messageTypeClusterMap.put(DefaultMessageTypes.REGISTER, mockMessageHandler);

        try {
            given(mockService.getMessageDispatcher(anyString())).willReturn(mockDispatcher);
        } catch (NotFoundException e) {
            //does not happen
        }
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();

        messageResource = new PushMessageResource(mockService, mockDebug);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldHandle() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException, PushNotificationException {

        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.AUTHENTICATE);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent(), mockMessageHandler);
        assertThat(result.get()).isNotNull();
    }

    @Test
    public void regShouldHandleByCTS() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException, CoreTokenException, PushNotificationException {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.REGISTER);

        //then
        assertThat(result.get()).isNotNull();
        verify(mockDispatcher, times(1)).handle("asdf", content, mockMessageHandler);
    }

    @Test
    public void authShouldHandleByCTS() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException, CoreTokenException, PushNotificationException {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.AUTHENTICATE);

        //then
        assertThat(result.get()).isNotNull();
        verify(mockDispatcher, times(1)).handle("asdf", content, mockMessageHandler);
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenNoMessageId() throws ResourceException, InterruptedException {

        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("test", "test")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.AUTHENTICATE);

        //then
        result.getOrThrow();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenPredicateNotMet() throws ResourceException, InterruptedException,
            PredicateNotMetException, PushNotificationException {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);

        doThrow(new PredicateNotMetException("")).when(mockDispatcher).handle("asdf", content, mockMessageHandler);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.AUTHENTICATE);

        //then
        result.getOrThrow();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenLocalAndCTSReadsFail() throws ResourceException, InterruptedException,
            PredicateNotMetException, CoreTokenException, PushNotificationException {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);

        doThrow(new NotFoundException()).when(mockDispatcher).handle("asdf", content,
                mockMessageHandler);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request,
                DefaultMessageTypes.AUTHENTICATE);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent(), mockMessageHandler);
        result.getOrThrow();
    }

}
