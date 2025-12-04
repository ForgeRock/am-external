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
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.secrets.Purpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
public class CaptchaNodeValidatorTest {

    private final List<String> path = List.of("somePath");
    @Mock
    private AnnotatedServiceRegistry serviceRegistry;
    @Mock
    private Realm realm;
    @Mock
    private CaptchaNode.Config existingConfig;

    @InjectMocks
    private CaptchaNode.ConfigValidator validator;

    @Test
    void throwsIfNeitherSecretKeyNorSecretKeyPurposeIsProvided() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(realm, path, emptyMap()))
                .isInstanceOf(ServiceConfigException.class);
    }

    @Test
    void doesNotThrowIfSecretKeyIsProvided() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.empty());

        validator.validate(realm, path, Map.of("secretKey", Set.of("someValue")));
    }

    @Test
    void doesNotThrowIfSecretKeyPurposeIsProvided() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.empty());

        validator.validate(realm, path, Map.of("secretKeyPurpose", Set.of("someValue")));
    }

    @Test
    void doesNotThrowIfExistingConfigContainsSecretKey() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(existingConfig));
        given(existingConfig.secretKey()).willReturn(Optional.of("someValue"));

        validator.validate(realm, path, emptyMap());
    }

    @Test
    void doesNotThrowIfExistingConfigContainsSecretKeyPurpose() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(existingConfig));
        given(existingConfig.secretKeyPurpose()).willReturn(Optional.of(Purpose.PASSWORD));

        validator.validate(realm, path, emptyMap());
    }

    @Test
    void throwsIfExistingValidConfigIsOverriddenByInvalidConfig() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(existingConfig));
        lenient().when(existingConfig.secretKey()).thenReturn(Optional.of("someValue"));
        lenient().when(existingConfig.secretKeyPurpose()).thenReturn(Optional.of(Purpose.PASSWORD));

        Map<String, Set<String>> invalidConfig = Map.of("secretKey", emptySet(), "secretKeyPurpose", emptySet());
        assertThatThrownBy(() -> validator.validate(realm, path, invalidConfig))
                .isInstanceOf(ServiceConfigException.class);
    }

    @Test
    void throwsIfExistingValidConfigIsOverriddenByInvalidBlankConfig() throws Exception {
        given(serviceRegistry.getRealmInstance(any(), any(), any())).willReturn(Optional.of(existingConfig));
        lenient().when(existingConfig.secretKey()).thenReturn(Optional.of("someValue"));
        lenient().when(existingConfig.secretKeyPurpose()).thenReturn(Optional.of(Purpose.PASSWORD));

        Map<String, Set<String>> invalidConfig = Map.of("secretKey", Set.of(" "), "secretKeyPurpose", Set.of("\n"));
        assertThatThrownBy(() -> validator.validate(realm, path, invalidConfig))
                .isInstanceOf(ServiceConfigException.class);
    }
}
