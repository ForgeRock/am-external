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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AttemptResponseCode.SUCCESS;
import static org.forgerock.secrets.GenericSecret.password;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;

import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.http.service.HttpClientService;
import org.forgerock.openam.http.service.HttpClientServiceProvider;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurIdRestClientTest {

    private final Purpose<GenericSecret> clientSecretPurpose = Purpose.purpose("label", GenericSecret.class);
    @Mock
    HttpClientService httpClientService;
    @Mock
    SecretCache secretCache;

    SecurIdRestClient client;

    @BeforeEach
    void setUp() {
        var httpClientServiceProvider = mock(HttpClientServiceProvider.class);
        var realm = mock(Realm.class);
        var secretReferenceCache = mock(SecretReferenceCache.class);
        given(httpClientServiceProvider.get(realm)).willReturn(httpClientService);
        given(secretReferenceCache.realm(realm)).willReturn(secretCache);
        client = new SecurIdRestClient(httpClientServiceProvider, realm, secretReferenceCache, "", clientSecretPurpose,
                0, "httpClient");
    }

    @Test
    void processesInitializeRequest() throws Exception {
        setUpClient();
        var response = new Response(Status.OK);
        response.setEntity(json(object(field("attemptResponseCode", "SUCCESS"))));
        given(httpClientService.send(any(), eq("httpClient"))).willReturn(newResultPromise(response));

        var result = client.initialize("username", "", "");

        assertThat(result.attemptResponseCode()).isEqualTo(SUCCESS);
    }

    @Test
    void processesVerifyRequest() throws Exception {
        setUpClient();
        var response = new Response(Status.OK);
        response.setEntity(json(object(field("attemptResponseCode", "SUCCESS"))));
        given(httpClientService.send(any(), eq("httpClient"))).willReturn(newResultPromise(response));

        var result = client.verify("responseMessageId", "authnAttemptId", null);

        assertThat(result.attemptResponseCode()).isEqualTo(SUCCESS);
    }

    private void setUpClient() {
        var secretsProvider = mock(SecretsProvider.class);
        given(secretsProvider.getActiveSecret(clientSecretPurpose))
                .willReturn(newResultPromise(password("password".toCharArray())));
        given(secretCache.active(clientSecretPurpose))
                .willReturn(SecretReference.active(secretsProvider, clientSecretPurpose, Clock.systemUTC()));
    }
}
