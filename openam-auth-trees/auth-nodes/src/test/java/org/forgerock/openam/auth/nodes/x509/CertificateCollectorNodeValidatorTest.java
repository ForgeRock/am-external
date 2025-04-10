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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.EITHER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.HEADER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.REQUEST;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorNodeValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CertificateCollectorNodeValidatorTest {

    private static final String HEADER_NAME = "test-header-name";

    @InjectMocks
    private CertificateCollectorNodeValidator validator;

    @Test
    void shouldValidateGivenCollectionMethodHeaderAndHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(HEADER.name()),
                "clientCertificateHttpHeaderName", Set.of(HEADER_NAME)
        );

        // When - Then
        assertThatNoException().isThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes));
    }

    @Test
    void shouldFailToValidateGivenCollectionMethodHeaderAndEmptyHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(HEADER.name()),
                "clientCertificateHttpHeaderName", Set.of("")
        );

        // When - Then
        assertThatThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("HTTP Header Name for Client Certificate is required for this collection method.");
    }

    @Test
    void shouldFailToValidateGivenCollectionMethodHeaderAndNoHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(HEADER.name())
        );

        // When - Then
        assertThatThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("HTTP Header Name for Client Certificate is required for this collection method.");
    }

    @Test
    void shouldValidateGivenCollectionMethodRequestAndHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(REQUEST.name()),
                "clientCertificateHttpHeaderName", Set.of(HEADER_NAME)
        );

        // When - Then
        assertThatNoException().isThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes));
    }

    @Test
    void shouldValidateGivenCollectionMethodRequestAndEmptyHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(REQUEST.name()),
                "clientCertificateHttpHeaderName", Set.of("")
        );

        // When - Then
        assertThatNoException().isThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes));
    }

    @Test
    void shouldValidateGivenCollectionMethodRequestAndNoHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(REQUEST.name())
        );

        // When - Then
        assertThatNoException().isThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes));
    }

    @Test
    void shouldValidateGivenCollectionMethodEitherAndHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(EITHER.name()),
                "clientCertificateHttpHeaderName", Set.of(HEADER_NAME)
        );

        // When - Then
        assertThatNoException().isThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes));
    }

    @Test
    void shouldFailToValidateGivenCollectionMethodEitherAndEmptyHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(EITHER.name()),
                "clientCertificateHttpHeaderName", Set.of("")
        );

        // When - Then
        assertThatThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("HTTP Header Name for Client Certificate is required for this collection method.");
    }

    @Test
    void shouldFailToValidateGivenCollectionMethodEitherAndNoHeaderName() {
        // Given
        Map<String, Set<String>> attributes = Map.of(
                "certificateCollectionMethod", Set.of(EITHER.name())
        );

        // When - Then
        assertThatThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("HTTP Header Name for Client Certificate is required for this collection method.");
    }

    @Test
    void shouldFailToValidateGivenNullAttributes() {

        // Given
        Map<String, Set<String>> attributes = null;

        // When - Then
        assertThatThrownBy(() -> validator.validate(mock(Realm.class), emptyList(), attributes))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Attributes are required for validation");
    }
}
