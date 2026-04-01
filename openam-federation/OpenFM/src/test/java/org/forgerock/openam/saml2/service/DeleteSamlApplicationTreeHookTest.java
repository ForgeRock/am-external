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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.service;

import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.RESPONSE_KEY_PARAM;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.SAML_OBJECT_KEY_PARAM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.forgerock.http.MutableUri;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.profile.SPCache;

class DeleteSamlApplicationTreeHookTest {

    @Test
    void shouldNotExecuteWhenNoRequestParamsPresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = new DeleteSamlApplicationTreeHook(null);

        // When
        boolean result = hook.shouldExecute();

        // Then
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void shouldExecuteWhenSamlObjectKeyIsPresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(SAML_OBJECT_KEY_PARAM, "saml-key"));

        // When
        boolean result = hook.shouldExecute();

        // Then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void shouldExecuteWhenResponseKeyIsPresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(RESPONSE_KEY_PARAM, "response-key"));

        // When
        boolean result = hook.shouldExecute();

        // Then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void shouldExecuteWhenSamlObjectKeyIsBlankAndResponseKeyIsPresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = hookWithParameters(
                Map.of(SAML_OBJECT_KEY_PARAM, " ", RESPONSE_KEY_PARAM, "response-key"));

        // When
        boolean result = hook.shouldExecute();

        // Then
        Assertions.assertThat(result).isTrue();
    }

    @Test
    void shouldNotExecuteWhenNoSamlKeysPresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of());

        // When
        boolean result = hook.shouldExecute();

        // Then
        Assertions.assertThat(result).isFalse();
    }

    @Test
    void shouldDeleteUsingSamlObjectKeyWhenPresent() {
        // Given
        String key = "saml-key";
        SPCache.samlDataHash.put(key, "saml-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(SAML_OBJECT_KEY_PARAM, key));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.accept();

            // Then
            failoverUtils.verify(() -> SAML2FailoverUtils.deleteSAML2Token(key));
            Assertions.assertThat(SPCache.samlDataHash.containsKey(key)).isFalse();
        } finally {
            SPCache.samlDataHash.remove(key);
        }
    }

    @Test
    void shouldDeleteUsingResponseKeyWhenSamlObjectKeyIsBlank() {
        // Given
        String key = "response-key";
        SPCache.samlDataHash.put(key, "saml-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(
                Map.of(SAML_OBJECT_KEY_PARAM, " ", RESPONSE_KEY_PARAM, key));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.accept();

            // Then
            failoverUtils.verify(() -> SAML2FailoverUtils.deleteSAML2Token(key));
            Assertions.assertThat(SPCache.samlDataHash.containsKey(key)).isFalse();
        } finally {
            SPCache.samlDataHash.remove(key);
        }
    }

    @Test
    void shouldPreferSamlObjectKeyOverResponseKeyWhenBothPresent() {
        // Given
        String samlKey = "saml-key";
        String responseKey = "response-key";
        SPCache.samlDataHash.put(samlKey, "saml-data");
        SPCache.samlDataHash.put(responseKey, "response-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(
                Map.of(SAML_OBJECT_KEY_PARAM, samlKey, RESPONSE_KEY_PARAM, responseKey));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.accept();

            // Then
            failoverUtils.verify(() -> SAML2FailoverUtils.deleteSAML2Token(samlKey));
            Assertions.assertThat(SPCache.samlDataHash.containsKey(samlKey)).isFalse();
            Assertions.assertThat(SPCache.samlDataHash.containsKey(responseKey)).isTrue();
        } finally {
            SPCache.samlDataHash.remove(samlKey);
            SPCache.samlDataHash.remove(responseKey);
        }
    }

    @Test
    void shouldNotAttemptDeleteWhenNoRequestParamsArePresent() {
        // Given
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of());

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.accept();

            // Then
            failoverUtils.verifyNoInteractions();
        }
    }

    @Test
    void shouldNotThrowWhenDeleteFails() {
        // Given
        String key = "saml-key";
        SPCache.samlDataHash.put(key, "saml-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(SAML_OBJECT_KEY_PARAM, key));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            failoverUtils.when(() -> SAML2FailoverUtils.deleteSAML2Token(key))
                    .thenThrow(new SAML2TokenRepositoryException("failed"));

            // When / Then
            Assertions.assertThatCode(hook::accept).doesNotThrowAnyException();
        } finally {
            SPCache.samlDataHash.remove(key);
        }
    }

    @Test
    void shouldNotDeleteOnFailureWhenSamlKeyIsPresent() throws Exception {
        // Given
        String key = "saml-key";
        SPCache.samlDataHash.put(key, "saml-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(SAML_OBJECT_KEY_PARAM, key));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.acceptFailure();

            // Then
            failoverUtils.verifyNoInteractions();
            Assertions.assertThat(SPCache.samlDataHash.containsKey(key)).isTrue();
        } finally {
            SPCache.samlDataHash.remove(key);
        }
    }

    @Test
    void shouldNotDeleteOnExceptionWhenSamlKeyIsPresent() throws Exception {
        // Given
        String key = "saml-key";
        SPCache.samlDataHash.put(key, "saml-data");
        DeleteSamlApplicationTreeHook hook = hookWithParameters(Map.of(SAML_OBJECT_KEY_PARAM, key));

        try (MockedStatic<SAML2FailoverUtils> failoverUtils = mockStatic(SAML2FailoverUtils.class)) {
            // When
            hook.acceptException();

            // Then
            failoverUtils.verifyNoInteractions();
            Assertions.assertThat(SPCache.samlDataHash.containsKey(key)).isTrue();
        } finally {
            SPCache.samlDataHash.remove(key);
        }
    }

    private DeleteSamlApplicationTreeHook hookWithParameters(Map<String, String> parameters) {
        Request request = mock(Request.class);
        Form queryParams = mock(Form.class);
        when(request.getUri()).thenReturn(mock(MutableUri.class));
        when(request.getQueryParams()).thenReturn(queryParams);
        when(queryParams.getFirst(anyString()))
                .thenAnswer(invocation -> parameters.get(invocation.getArgument(0, String.class)));
        return new DeleteSamlApplicationTreeHook(request);
    }
}
