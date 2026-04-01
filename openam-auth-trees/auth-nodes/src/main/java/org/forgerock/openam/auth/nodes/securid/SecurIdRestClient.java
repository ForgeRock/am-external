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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.securid;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.fieldIfNotNull;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.http.service.HttpClientService;
import org.forgerock.openam.http.service.HttpClientServiceProvider;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A client for interacting with the SecurID REST API.
 */
class SecurIdRestClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClientService httpClientService;
    private final SecretCache secrets;
    private final String initializePath;
    private final String verifyPath;
    private final Purpose<GenericSecret> clientKeyPurpose;
    private final int authnAttemptTimeout;
    private final String httpClient;

    @Inject
    SecurIdRestClient(HttpClientServiceProvider httpClientServiceProvider,
                      @Assisted Realm realm,
                      SecretReferenceCache secrets,
                      @Assisted("baseUrl") String baseUrl,
                      @Assisted Purpose<GenericSecret> clientKeyPurpose,
                      @Assisted int authnAttemptTimeout,
                      @Assisted("httpClient") String httpClient) {
        this.httpClientService = requireNonNull(httpClientServiceProvider).get(realm);
        this.secrets = requireNonNull(secrets.realm(realm));
        initializePath = baseUrl + "/authn/initialize";
        verifyPath = baseUrl + "/authn/verify";
        this.clientKeyPurpose = clientKeyPurpose;
        this.authnAttemptTimeout = authnAttemptTimeout;
        this.httpClient = httpClient;
    }

    SecurIdResponse initialize(String username, String clientId, String assurancePolicy)
            throws NodeProcessException, URISyntaxException, IOException, NoSuchSecretException {
        JsonValue body = getInitializeBody(username, clientId, assurancePolicy, authnAttemptTimeout);
        var request = new Request()
                .setMethod("POST")
                .setUri(initializePath)
                .setEntity(body.getObject());
        var response = sendRequest(request);
        if (!response.getStatus().isSuccessful()) {
            throw new NodeProcessException("Failed to initialize SecurID: " + response.getStatus().getReasonPhrase());
        }
        return MAPPER.readValue(response.getEntity().getString(), SecurIdResponse.class);
    }

    private static JsonValue getInitializeBody(String subject, String clientID, String assurancePolicy,
                                               int authnAttemptTimeout) {
        return json(object(
                field("context", object(
                        field("messageId", randomUUID().toString())
                )),
                fieldIfNotNull("clientId", clientID),
                fieldIfNotNull("assurancePolicyId", assurancePolicy),
                field("subjectName", subject),
                field("authnAttemptTimeout", authnAttemptTimeout)
        ));
    }

    SecurIdResponse verify(String responseMessageId, String authnAttemptId, List<SubjectCredentials> subjectCredentials)
            throws URISyntaxException, IOException, NodeProcessException, NoSuchSecretException {
        JsonValue contextBody = getContext(responseMessageId, authnAttemptId);
        JsonValue body = json(object(field("context", contextBody), field("subjectCredentials", subjectCredentials)));

        var request = new Request().setMethod("POST").setUri(verifyPath).setEntity(body);
        var response = sendRequest(request);
        if (!response.getStatus().isSuccessful()) {
            throw new NodeProcessException("Failed to verify SecurID: " + response.getStatus().getReasonPhrase());
        }
        return MAPPER.readValue(response.getEntity().getString(), SecurIdResponse.class);
    }

    private static JsonValue getContext(String inResponseTo, String authnAttemptId) {
        return json(object(
                field("authnAttemptId", authnAttemptId),
                field("messageId", randomUUID().toString()),
                field("inResponseTo", inResponseTo)
        ));
    }

    private Response sendRequest(Request request) throws NoSuchSecretException {
        var clientKey = secrets.active(clientKeyPurpose)
                                .get()
                                .revealAsUtf8(String::new);
        request.addHeaders(new GenericHeader("client-key", clientKey));
        return httpClientService.send(request, httpClient).getOrThrowIfInterrupted();
    }

    /**
     * Factory interface for creating instances of {@link SecurIdRestClient}.
     */
    interface Factory {
        SecurIdRestClient create(Realm realm,
                                 @Assisted("baseUrl") String baseUrl,
                                 Purpose<GenericSecret> clientKeyPurpose,
                                 int authnAttemptTimeout,
                                 @Assisted("httpClient") String httpClient);
    }
}
