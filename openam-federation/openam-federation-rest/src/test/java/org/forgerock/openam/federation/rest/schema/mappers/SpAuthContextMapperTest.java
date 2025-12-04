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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.federation.rest.schema.hosted.service.AuthContextItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpAuthContextMapperTest {

    private SpAuthContextMapper mapper = new SpAuthContextMapper();

    @Mock
    private AuthContextItem authContextItem;


    @Test
    void shouldConvertSimpleEntry() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|3|"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getLevel()).isEqualTo(3);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        assertThat(entry.getDefaultItem()).isFalse();
    }

    @Test
    void shouldConvertEntryWithoutLevel() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport||"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getLevel()).isEqualTo(0);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        assertThat(entry.getDefaultItem()).isFalse();
    }

    @Test
    void shouldConvertEntryWithAllFieldsCompleted() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|10|default"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getLevel()).isEqualTo(10);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        assertThat(entry.getDefaultItem()).isTrue();
    }

    @Test
    void shouldInverseMappedValue() {
        // Given
        given(authContextItem.getContextReference())
                .willReturn("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        given(authContextItem.getLevel()).willReturn(5);
        given(authContextItem.getDefaultItem()).willReturn(true);
        List<AuthContextItem> mappedValue = new ArrayList<>();
        mappedValue.add(authContextItem);

        // When
        List<String> actual = mapper.inverse(mappedValue, ROOT);

        // Then
        assertThat(actual.get(0)).isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|5|default");
    }

    @Test
    void shouldInverseMappedValueNonDefaultAndWithoutLevel() {
        // Given
        given(authContextItem.getContextReference())
                .willReturn("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        given(authContextItem.getLevel()).willReturn(null);
        given(authContextItem.getDefaultItem()).willReturn(false);
        List<AuthContextItem> mappedValue = new ArrayList<>();
        mappedValue.add(authContextItem);

        // When
        List<String> actual = mapper.inverse(mappedValue, ROOT);

        // Then
        assertThat(actual.get(0)).isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|0|");
    }
}
