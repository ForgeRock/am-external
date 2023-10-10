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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.http.header.SetCookie2Header;
import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class JavaScriptHttpClientTest {

    @Test
    public void httpGetShouldPropagateTransactionId() throws Exception {
        // given
        resetAuditRequestContext();
        Handler mockHandler = mock(Handler.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        given(mockHandler.handle(any(Context.class), requestCaptor.capture())).willReturn(newResultPromise(responseOK()));

        JavaScriptHttpClient httpClient = new JavaScriptHttpClient(new Client(mockHandler), new HttpClientRequestFactory());

        // when
        httpClient.get("https://www.example.com", null);

        // then
        assertThat(requestCaptor.getValue()).isNotNull();
        assertPropagatesTransactionId(requestCaptor.getValue());
    }

    @Test
    public void httpPostShouldPropagateTransactionId() throws Exception {
        // given
        resetAuditRequestContext();
        Handler mockHandler = mock(Handler.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        given(mockHandler.handle(any(Context.class), requestCaptor.capture())).willReturn(newResultPromise(responseOK()));

        JavaScriptHttpClient httpClient = new JavaScriptHttpClient(new Client(mockHandler), new HttpClientRequestFactory());

        // when
        httpClient.post("https://www.example.com", null, null);

        // then
        assertThat(requestCaptor.getValue()).isNotNull();
        assertPropagatesTransactionId(requestCaptor.getValue());
    }

    @Test
    public void sendShouldPropagateTransactionId() throws Exception {
        // given
        resetAuditRequestContext();
        Handler mockHandler = mock(Handler.class);
        Request req = new Request();
        req.setUri("https://www.example.com");
        req.setMethod("GET");
        given(mockHandler.handle(any(Context.class), eq(req))).willReturn(newResultPromise(responseOK()));

        JavaScriptHttpClient httpClient = new JavaScriptHttpClient(new Client(mockHandler), new HttpClientRequestFactory());

        // when
        httpClient.send(req);

        // then
        assertPropagatesTransactionId(req);
    }

    private static void resetAuditRequestContext() {
        AuditRequestContext.setAuditRequestContext(new AuditRequestContext());
    }

    private static Response responseOK() {
        Response response = new Response(Status.OK);
        // Need to set cookie headers to avoid NPE in ChfHttpClient.createHttpClientResponse:
        response.getHeaders().put(new SetCookieHeader(List.of(new Cookie())));
        response.getHeaders().add(new SetCookie2Header(List.of(new Cookie())));
        return response;
    }

    private static void assertPropagatesTransactionId(Request req) {
        String txId = AuditRequestContext.getAuditRequestContext().getTransactionIdValue();
        String subTxId = txId+"/0";
        assertThat(req.getHeaders().get("X-ForgeRock-TransactionId").getFirstValue()).isEqualTo(subTxId);
    }

}