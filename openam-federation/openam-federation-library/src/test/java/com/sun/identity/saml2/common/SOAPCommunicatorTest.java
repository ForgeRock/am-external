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
 * Copyright 2024-2026 ForgeRock AS.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.http.protocol.Entity;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SOAPCommunicatorTest {

    @Mock
    private Response response;
    @Mock
    private Entity entity;
    @Mock
    private InputStream inputStream;
    @Mock
    private MessageFactory messageFactory;
    @Mock
    private SOAPMessage soapMessage;
    @Captor
    private ArgumentCaptor<MimeHeaders> captor;

    private SOAPCommunicator soapCommunicator;
    private MessageFactory originalMessageFactory;

    @BeforeEach
    void setUp() throws Exception {
        soapCommunicator = SOAPCommunicator.getInstance();
        Field messageFactoryField = SOAPCommunicator.class.getDeclaredField("messageFactory");
        messageFactoryField.setAccessible(true);
        originalMessageFactory = (MessageFactory) messageFactoryField.get(soapCommunicator);
        messageFactoryField.set(soapCommunicator, messageFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field messageFactoryField = SOAPCommunicator.class.getDeclaredField("messageFactory");
        messageFactoryField.setAccessible(true);
        messageFactoryField.set(soapCommunicator, originalMessageFactory);
    }

    @Test
    void testResponseHeadersPassedToCreateMessage() throws SOAPException, IOException {
        //given
        Headers testHeaders = new Headers();
        testHeaders.add("Content-Length", "0");
        testHeaders.add("Content-Type", "text/xml");
        testHeaders.add("Authorization", "Basic dXNlcjpwYXNzd29yZA==");
        given(response.getEntity()).willReturn(entity);
        given(response.getHeaders()).willReturn(testHeaders);
        given(entity.getRawContentInputStream()).willReturn(inputStream);
        given(messageFactory.createMessage(captor.capture(), any())).willReturn(soapMessage);

        //when
        soapCommunicator.getSOAPMessage(response);

        //then
        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");
        mimeHeaders.addHeader("Content-Length", "0");
        mimeHeaders.addHeader("Content-Type", "text/xml");

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(mimeHeaders);
    }
}