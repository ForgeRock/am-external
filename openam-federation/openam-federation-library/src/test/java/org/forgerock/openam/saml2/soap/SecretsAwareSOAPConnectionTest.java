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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.soap;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import java.net.URI;
import java.net.URISyntaxException;

@ExtendWith(MockitoExtension.class)
class SecretsAwareSOAPConnectionTest {

    @Mock
    private SamlMtlsHandlerFactory samlMtlsHandlerFactory;
    @Mock
    SOAPCommunicator soapCommunicator;
    @Mock
    private Realm realm;
    @Mock
    private SOAPMessage soapMessage;
    @Mock
    private Handler handler;
    @Mock
    private Promise<Response, NeverThrowsException> responsePromise;
    @Captor
    private ArgumentCaptor<Request> captor;

    private final String location = "https://am.example.com";
    private SecretsAwareSOAPConnection secretsAwareSOAPConnection;

    @BeforeEach
    void setUp() {
        MockedStatic<SAML2Utils> saml2UtilsMockedStatic = mockStatic(SAML2Utils.class);
        saml2UtilsMockedStatic.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");
        secretsAwareSOAPConnection = new SecretsAwareSOAPConnection(samlMtlsHandlerFactory, realm,
                soapCommunicator, "test", SP);
        saml2UtilsMockedStatic.close();
    }

    @Test
    void testRejectsInvalidURI() throws SOAPException {
        //given
        SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, new Object());

        //then
        assertThrows(IllegalArgumentException.class, soapConnectionCall);
    }

    @Test
    void testURIObjectIsNotRejected() throws URISyntaxException {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.OK);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);
        URI uri = new URI(location);

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, uri);

        //then
        assertDoesNotThrow(soapConnectionCall);
    }

    @Test
    void testStringURINotRejected() {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.OK);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, location);

        //then
        assertDoesNotThrow(soapConnectionCall);
    }

    @Test
    void testThrowsSOAPExceptionIfUnexpectedResponse() {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.BAD_REQUEST);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, location);

        //then
        assertThrows(SOAPException.class, soapConnectionCall);
    }

    @Test
    void testAcceptsOKResponse() {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.OK);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, location);

        //then
        assertDoesNotThrow(soapConnectionCall);
    }

    @Test
    void testAcceptsInternalServerErrorResponse() {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.INTERNAL_SERVER_ERROR);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);

        //when
        Executable soapConnectionCall = () -> secretsAwareSOAPConnection.call(soapMessage, location);

        //then
        assertDoesNotThrow(soapConnectionCall);
    }

    @Test
    void testMaintainsHeaders() throws SOAPException {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.OK);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);

        //when
        secretsAwareSOAPConnection.call(soapMessage, location);

        //then
        Headers testHeaders = new Headers();
        testHeaders.add("Content-Length", "0");
        testHeaders.add("Content-Type", "text/xml");

        assertThat(captor.getValue().getHeaders())
                .usingRecursiveComparison()
                .isEqualTo(testHeaders);
    }

    @Test
    void testAddsBasicAuth() throws SOAPException {
        //given
        SOAPMessage soapMessage = setupCommonStubs();
        Response response = new Response(Status.OK);
        given(responsePromise.getOrThrowIfInterrupted()).willReturn(response);
        String basicAuthURI = "https://user:password@example.com";

        //when
        secretsAwareSOAPConnection.call(soapMessage, basicAuthURI);

        //then
        Headers testHeaders = new Headers();
        testHeaders.add("Content-Length", "0");
        testHeaders.add("Content-Type", "text/xml");
        testHeaders.add("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        assertThat(captor.getValue().getHeaders())
                .usingRecursiveComparison()
                .isEqualTo(testHeaders);
    }


    private SOAPMessage setupCommonStubs() {
        MimeHeaders mimeHeaders = new MimeHeaders();
        mimeHeaders.addHeader("Content-Type", "text/xml");
        given(soapMessage.getMimeHeaders()).willReturn(mimeHeaders);
        given(samlMtlsHandlerFactory.getHandler(any(), any())).willReturn(handler);
        given(handler.handle(any(Context.class), captor.capture())).willReturn(responsePromise);
        return soapMessage;
    }
}
