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
package org.forgerock.openam.auth.trees.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.openam.auth.trees.validation.AuthTreeValidationEngine;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.feature.Features;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.sm.ServiceSchema;

@ExtendWith(MockitoExtension.class)
class AuthTreesCollectionProviderTest {
    @Mock
    private Features features;
    @Mock
    private ServiceSchema schema;
    @Mock
    private SchemaGeneratorFactory schemaGeneratorFactory;
    @Mock
    private SmsJsonConverterFactory converterFactory;
    @Mock
    private AuthTreeValidationEngine validationEngine;
    @InjectMocks
    private AuthTreesCollectionProvider authTreesCollectionProvider;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(Context.class);
        var mockRealmContext = mock(RealmContext.class);
        given(mockContext.asContext(RealmContext.class)).willReturn(mockRealmContext);
        given(mockRealmContext.getRealm()).willReturn(mock(Realm.class));
    }

    @Test
    void validateTreeShouldReturnValidTrueWhenNoValidationErrorsOrWarnings() throws Exception {
        // given
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(json(object()));
        given(validationEngine.validate(any(), any())).willReturn(emptyList());

        // when
        JsonValue responseContent = authTreesCollectionProvider.validateTree(request, mockContext).getOrThrow()
                                            .getJsonContent();

        // then
        assertThat(responseContent.get("valid").asBoolean()).isTrue();
        assertThat(responseContent.get("warnings").asList()).isEmpty();
        assertThat(responseContent.get("errors").asList()).isEmpty();
    }

    @Test
    void validateTreeShouldReturnValidFalseWhenValidationErrors() throws Exception {
        // given
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(json(object()));
        ValidationFailure error = new ValidationFailure("error-message", ValidationFailure.Severity.ERROR);
        given(validationEngine.validate(any(), any())).willReturn(singletonList(error));

        // when
        JsonValue responseContent = authTreesCollectionProvider.validateTree(request, mockContext).getOrThrow()
                                            .getJsonContent();

        // then
        assertThat(responseContent.get("valid").asBoolean()).isFalse();
        assertThat(responseContent.get("errors").asList()).containsExactly("error-message");
        assertThat(responseContent.get("warnings").asList()).isEmpty();
    }

    @Test
    void validateTreeShouldReturnValidTrueWhenValidationWarnings() throws Exception {
        // given
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(json(object()));
        ValidationFailure warning = new ValidationFailure("warning-message", ValidationFailure.Severity.WARNING);
        given(validationEngine.validate(any(), any())).willReturn(singletonList(warning));

        // when
        ActionResponse response = authTreesCollectionProvider.validateTree(request, mockContext).getOrThrow();

        // then
        assertThat(response.getJsonContent().get("valid").asBoolean()).isTrue();
        assertThat(response.getJsonContent().get("warnings").asList()).containsExactly("warning-message");
        assertThat(response.getJsonContent().get("errors").asList()).isEmpty();
    }

    @Test
    void validateTreeShouldReturnValidFalseWhenValidationErrorsAndWarnings() throws Exception {
        // given
        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(json(object()));
        ValidationFailure error = new ValidationFailure("error-message", ValidationFailure.Severity.ERROR);
        ValidationFailure warning = new ValidationFailure("warning-message", ValidationFailure.Severity.WARNING);
        given(validationEngine.validate(any(), any())).willReturn(List.of(error, warning));

        // when
        ActionResponse response = authTreesCollectionProvider.validateTree(request, mockContext).getOrThrow();

        // then
        assertThat(response.getJsonContent().get("valid").asBoolean()).isFalse();
        assertThat(response.getJsonContent().get("errors").asList()).containsExactly("error-message");
        assertThat(response.getJsonContent().get("warnings").asList()).containsExactly("warning-message");
    }
}