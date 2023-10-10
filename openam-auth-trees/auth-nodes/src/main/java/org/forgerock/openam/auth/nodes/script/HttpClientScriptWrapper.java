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
package org.forgerock.openam.auth.nodes.script;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.forgerock.http.Client;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Headers;
import org.forgerock.http.protocol.Request;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.TransactionId;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;

/**
 * A wrapper class to simplify sending HTTP requests in scripts.
 */
@Supported
public class HttpClientScriptWrapper {

    private final Client httpClient;

    /**
     * Constructs the HttpClientScriptWrapper.
     *
     * @param httpClient The httpClient to wrap.
     */
    public HttpClientScriptWrapper(Client httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sends an asynchronous GET request to the given URI with no additional options.
     *
     * @param uri The URI to send the request to.
     * @return A promise which will resolve to a wrapped Response object.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Promise<ResponseScriptWrapper, HttpClientScriptException> send(String uri) {
        return send(uri, Collections.emptyMap());
    }

    /**
     * Sends an asynchronous request to the given URI alongside the given request options.
     * <p>
     * Supported request options are:
     *  <ul>
     *      <li>method: GET, POST etc.</li>
     *      <li>headers: a map of header names to their values</li>
     *      <li>token: a bearer token attached as an Authorization header</li>
     *      <li>body: the body of the request</li>
     *  </ul>
     * </p>
     *
     * @param uri            The URI to send the request to.
     * @param requestOptions A Native JS object containing fields which will modify the request.
     * @return A promise which will resolve to a wrapped Response object.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Promise<ResponseScriptWrapper, HttpClientScriptException> send(String uri,
            Map<String, Object> requestOptions) {
        try {
            Request request = constructRequest(uri, requestOptions);
            return httpClient.send(request)
                    .then(ResponseScriptWrapper::new)
                    .thenCatch(e -> {
                        throw new HttpClientScriptException(e);
                    })
                    .thenCatchRuntimeExceptionAsync(e ->
                            Promises.newExceptionPromise(new HttpClientScriptException(e)));
        } catch (URISyntaxException e) {
            return Promises.newExceptionPromise(new HttpClientScriptException(e));
        }
    }

    private Request constructRequest(String uri, Map<String, Object> requestOptions) throws URISyntaxException {
        Request request = new Request();
        request.setUri(uri);
        request.setMethod((String) requestOptions.getOrDefault("method", "GET"));
        request.putHeaders(extractHeaders(requestOptions));
        request.setEntity(requestOptions.get("body"));
        return request;
    }

    private Header[] extractHeaders(Map<String, Object> requestOptions) {
        Map<String, Object> requestHeaders = (Map) requestOptions.getOrDefault("headers", Collections.emptyMap());
        Headers headers = new Headers();
        headers.addAll(requestHeaders);

        TransactionId subTransactionId = new TransactionId(getAuditRequestContext().createSubTransactionIdValue());
        headers.add(new TransactionIdHeader(subTransactionId));

        String requestToken = requestOptions.getOrDefault("token", "").toString();
        if (StringUtils.isNotBlank(requestToken)) {
            headers.add("Authorization", String.format("Bearer %s", requestToken));
        }

        return headers.values().toArray(new Header[0]);
    }

}