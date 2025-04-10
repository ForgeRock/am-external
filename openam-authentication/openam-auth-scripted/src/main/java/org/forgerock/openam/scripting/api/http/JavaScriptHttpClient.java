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
 * Copyright 2010-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.scripting.api.http;

import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;
import static org.forgerock.openam.scripting.api.http.ScriptHttpClientFactory.SCRIPTING_HTTP_CLIENT_NAME;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.http.Client;
import org.forgerock.http.client.ChfHttpClient;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.http.client.response.HttpClientResponse;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HTTP Rest client for JavaScript auth module.
 *
 * @deprecated Will be replaced in a later release by {@link Client}.
 */
@Deprecated
public class JavaScriptHttpClient extends ChfHttpClient {

    private static final Logger DEBUG = LoggerFactory.getLogger(JavaScriptHttpClient.class);

    @Inject
    public JavaScriptHttpClient(@Named(SCRIPTING_HTTP_CLIENT_NAME) Client client, HttpClientRequestFactory httpClientRequestFactory) {
        super(client, httpClientRequestFactory);
    }

    /**
     * @param uri URI of resource to be accessed
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse get(String uri, NativeObject requestData)
            throws IOException, MalformedHeaderException,
            URISyntaxException {
        DEBUG.warn("'get' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, null, convertRequestData(requestData), "GET");
    }

    /**
     * @param uri URI of resource to be accessed
     * @param body The body of the http request
     * @param requestData Data to be sent during the request
     * @return The response from the REST call
     * @throws UnsupportedEncodingException
     */
    public HttpClientResponse post(String uri, String body, NativeObject requestData)
            throws IOException, MalformedHeaderException, URISyntaxException {
        DEBUG.warn("'post' has been deprecated. Use 'send' instead");
        return getHttpClientResponse(uri, body, convertRequestData(requestData), "POST");
    }

    /**
     * Sends an HTTP request and returns a {@code Promise} representing the
     * pending HTTP response.
     *
     * @param request
     *            The HTTP request to send.
     * @return A promise representing the pending HTTP response.
     */
    public Promise<Response, NeverThrowsException> send(final Request request) {
        request.getHeaders().add(new TransactionIdHeader(getAuditRequestContext().createSubTransactionId()));
        return client.send(request);
    }

    private Map<String, List<Map<String, String>>> convertRequestData(NativeObject requestData) {
        if (requestData == null) {
            return new HashMap<>();
        }

        Map<String, List<Map<String, String>>> convertedRequestData = new HashMap<>();
        NativeArray cookies = (NativeArray) NativeObject.getProperty(requestData, "cookies");
        List<Map<String, String>> convertedCookies = new ArrayList<>();
        if (cookies != null) {
            for (Object id : cookies.getIds()) {
                NativeObject cookie = (NativeObject) cookies.get((Integer) id, null);
                String domain = (String) cookie.get("domain", null);
                String field = (String) cookie.get("field", null);
                String value = (String) cookie.get("value", null);

                convertedCookies.add(convertCookie(domain, field, value));
            }
        }
        convertedRequestData.put("cookies", convertedCookies);

        NativeArray headers = (NativeArray) NativeObject.getProperty(requestData, "headers");
        List<Map<String, String>> convertedHeaders = new ArrayList<>();
        if (headers != null) {
            for (Object id : headers.getIds()) {
                NativeObject header = (NativeObject) headers.get((Integer) id, null);
                String field = (String) header.get("field", null);
                String value = (String) header.get("value", null);

                convertedHeaders.add(convertHeader(field, value));
            }
        }
        convertedRequestData.put("headers", convertedHeaders);
        return convertedRequestData;
    }

    private HashMap<String, String> convertHeader(String field, String value) {
        HashMap<String, String> convertedHeader = new HashMap<>();
        convertedHeader.put("field", field);
        convertedHeader.put("value", value);
        return convertedHeader;
    }

    private HashMap<String,String> convertCookie(String domain, String field, String value) {
        HashMap<String,String> convertedCookie = new HashMap<>();
        convertedCookie.put("domain", domain);
        convertedCookie.put("field", field);
        convertedCookie.put("value", value);
        return convertedCookie;
    }
}
