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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.services.push.sns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.MessageType;
import org.forgerock.openam.services.push.PushMessageResource;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.PredicateNotMetException;
import org.forgerock.openam.services.push.utils.PushDeviceUpdater;
import org.forgerock.util.promise.Promise;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PushMessageResourceTest {

    private PushMessageResource messageResource;
    private PushNotificationService mockService;
    private MessageDispatcher mockDispatcher;
    private RealmTestHelper realmTestHelper;
    private ClusterMessageHandler mockMessageHandler;
    private Map<MessageType, ClusterMessageHandler> messageTypeClusterMap;
    private MessageIdFactory mockMessageIdFactory;
    private MessageId mockMessageId;
    private PushDeviceUpdater mockPushDeviceUpdater;

    @BeforeEach
    void setup() throws Exception {
        mockService = mock(PushNotificationService.class);
        mockDispatcher = mock(MessageDispatcher.class);
        mockMessageHandler = mock(ClusterMessageHandler.class);
        mockMessageIdFactory = mock(MessageIdFactory.class);
        mockMessageId = mock(MessageId.class);
        mockPushDeviceUpdater = mock(PushDeviceUpdater.class);

        messageTypeClusterMap = new HashMap<>();
        messageTypeClusterMap.put(DefaultMessageTypes.AUTHENTICATE, mockMessageHandler);
        messageTypeClusterMap.put(DefaultMessageTypes.REGISTER, mockMessageHandler);

        given(mockService.getMessageDispatcher(anyString())).willReturn(mockDispatcher);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();

        messageResource = new PushMessageResource(mockService, mockMessageIdFactory,
                mockPushDeviceUpdater);
    }

    @AfterEach
    void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    void shouldHandle() throws Exception {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);
        prepareMocks(realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle(mockMessageId, request.getContent(), mockMessageHandler);
        assertThat(result.get()).isNotNull();
    }

    @Test
    void handleShouldInvokeMessageDispatcher() throws Exception {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");

        RealmContext realmContext = mockRealmContext(realm);

        prepareMocks(realm);
        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request);

        //then
        assertThat(result.get()).isNotNull();
        verify(mockDispatcher, times(1)).handle(mockMessageId, content, mockMessageHandler);
    }

    @Test
    void shouldFailWhenNoMessageId() {

        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);

        JsonValue content = JsonValue.json(object(field("test", "test")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request);

        //then
        assertThatThrownBy(result::getOrThrow)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bad request.");
    }

    @Test
    void shouldFailWhenPredicateNotMet() throws Exception {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        prepareMocks(realm);

        doThrow(new PredicateNotMetException(""))
                .when(mockDispatcher)
                .handle(mockMessageId, content, mockMessageHandler);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request);

        //then
        assertThatThrownBy(result::getOrThrow)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bad request.");
    }

    @Test
    void shouldFailWhenLocalAndCTSReadsFail() throws Exception {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        prepareMocks(realm);

        doThrow(new NotFoundException()).when(mockDispatcher).handle(mockMessageId, content, mockMessageHandler);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.handle(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle(mockMessageId, request.getContent(), mockMessageHandler);
        assertThatThrownBy(result::getOrThrow)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bad request.");
    }

    @Test
    void shouldUpdateWithUsername() throws Exception {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);

        JsonValue content = JsonValue.json(object(
                field("mechanismUid", "mechanism-uid"),
                field("username", "some-user"),
                field("jwt", "")
        ));
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        doReturn(getDeviceSettings()).when(mockPushDeviceUpdater).getDeviceSettings(any(), any(), anyString());
        doReturn(true).when(mockPushDeviceUpdater).validateSignedJwt(any(), any(), any());
        doReturn(true).when(mockPushDeviceUpdater).updateDevice(any(), anyString());

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.update(realmContext, request);

        //then
        verify(mockPushDeviceUpdater, times(1))
                .getDeviceSettings(anyString(), anyString(), anyString());
        verify(mockPushDeviceUpdater, times(1))
                .validateSignedJwt(any(), any(), any());
        verify(mockPushDeviceUpdater, times(1))
                .updateDevice(any(), anyString());
        verify(mockPushDeviceUpdater, times(1))
                .saveDeviceSettings(any(), any(), anyString(), anyString());
        assertThat(result.get()).isNotNull();
    }

    @Test
    void shouldFailUpdateWhenNoMechanismId() {
        //given
        Realm realm = realmTestHelper.mockRealm("realm");
        RealmContext realmContext = mockRealmContext(realm);

        JsonValue content = JsonValue.json(object(field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.update(realmContext, request);

        //then
        assertThatThrownBy(result::getOrThrow)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bad request.");
    }

    private RealmContext mockRealmContext(Realm realm) {
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        return new RealmContext(mockSSOTokenContext, realm);
    }

    private void prepareMocks(Realm realm) throws PushNotificationException {
        given(mockMessageIdFactory.create(eq("asdf"), any())).willReturn(mockMessageId);
        given(mockMessageId.getMessageType()).willReturn(DefaultMessageTypes.AUTHENTICATE);
        given(mockService.getMessageHandlers(realm.asPath())).willReturn(messageTypeClusterMap);
    }

    private PushDeviceSettings getDeviceSettings() {
        PushDeviceSettings settings = new PushDeviceSettings();
        settings.setSharedSecret("shared-secret");
        settings.setDeviceName("device-name");
        settings.setDeviceId("device-id");
        settings.setCommunicationId("communication-id");
        settings.setDeviceMechanismUID("mechanism-uid");
        settings.setCommunicationType("communication-type");
        settings.setDeviceType("device-type");
        return settings;
    }
}