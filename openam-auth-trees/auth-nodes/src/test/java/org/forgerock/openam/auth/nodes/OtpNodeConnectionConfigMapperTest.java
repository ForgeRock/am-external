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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.SecretsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class OtpNodeConnectionConfigMapperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
    private static final String CONFIG_PASSWORD = "config-password";
    private static final String SECRETS_PASSWORD = "secrets-password";

    @Mock
    private SecretReferenceCache secretReferenceCache;
    @Mock
    private OtpNodeBaseConfig config;
    @Mock
    private Realm realm;
    @Mock
    private SecretCache realmCache;
    @InjectMocks
    private OtpNodeConnectionConfigMapper mapper;


    @Before
    public void setUp() {
        given(config.sslOption()).willReturn(OtpNodeBaseConfig.SslOption.SSL);
        given(secretReferenceCache.realm(realm)).willReturn(realmCache);
    }

    @Test
    public void createsMapFromConfigObject() {
        given(config.hostName()).willReturn("hostName");
        given(config.hostPort()).willReturn(1234);
        given(config.username()).willReturn("userName");
        given(config.sslOption()).willReturn(OtpNodeBaseConfig.SslOption.SSL);
        given(config.password()).willReturn(Optional.of(CONFIG_PASSWORD.toCharArray()));

        Map<String, Set<String>> map =  mapper.asConfigMap(config, realm);

        assertThat(map).contains(Map.entry("sunAMAuthHOTPSMTPHostName", Set.of("hostName")),
                Map.entry("sunAMAuthHOTPSMTPHostPort", Set.of("1234")),
                Map.entry("sunAMAuthHOTPSMTPUserName", Set.of("userName")),
                Map.entry("sunAMAuthHOTPSMTPSSLEnabled", Set.of("SSL")),
                Map.entry("sunAMAuthHOTPSMTPUserPassword", Set.of(CONFIG_PASSWORD)));
    }

    @Test
    public void shouldUseSecretStorePasswordIfBothPasswordsArePresentAndSecretIsPresentInTheSecretStore() {
        char[] passwordCharArray = CONFIG_PASSWORD.toCharArray();
        given(config.passwordPurpose()).willReturn(Optional.of(Purpose.PASSWORD));
        lenient().when(config.password()).thenReturn(Optional.of(passwordCharArray));

        SecretsProvider secretsProvider = new SecretsProvider(CLOCK);
        secretsProvider.useSpecificSecretForPurpose(Purpose.PASSWORD,
                GenericSecret.password(SECRETS_PASSWORD.toCharArray()));

        given(realmCache.active(Purpose.PASSWORD))
                .willReturn(SecretReference.active(secretsProvider, Purpose.PASSWORD, CLOCK));

        Map<String, Set<String>> map = mapper.asConfigMap(config, realm);
        assertThat(map).extractingByKey("sunAMAuthHOTPSMTPUserPassword").isEqualTo(Set.of(SECRETS_PASSWORD));
    }

    @Test
    public void shouldUsePasswordPurposeIfOnlyPasswordPurposeIsPresentInTheSecretStore() {
        given(config.passwordPurpose()).willReturn(Optional.of(Purpose.PASSWORD));

        SecretsProvider secretsProvider = new SecretsProvider(CLOCK);
        secretsProvider.useSpecificSecretForPurpose(Purpose.PASSWORD,
                GenericSecret.password(SECRETS_PASSWORD.toCharArray()));

        given(realmCache.active(Purpose.PASSWORD))
                .willReturn(SecretReference.active(secretsProvider, Purpose.PASSWORD, CLOCK));

        Map<String, Set<String>> map = mapper.asConfigMap(config, realm);
        assertThat(map).extractingByKey("sunAMAuthHOTPSMTPUserPassword").isEqualTo(Set.of(SECRETS_PASSWORD));
    }

    @Test
    public void shouldUseConfigPasswordIfBothPasswordsArePresentButPasswordIsNotPresentInTheSecretStore() {
        char[] passwordCharArray = CONFIG_PASSWORD.toCharArray();
        given(config.passwordPurpose()).willReturn(Optional.of(Purpose.PASSWORD));
        given(config.password()).willReturn(Optional.of(passwordCharArray));

        SecretsProvider secretsProvider = new SecretsProvider(CLOCK);

        given(realmCache.active(Purpose.PASSWORD))
                .willReturn(SecretReference.active(secretsProvider, Purpose.PASSWORD, CLOCK));

        Map<String, Set<String>> map = mapper.asConfigMap(config, realm);
        assertThat(map).extractingByKey("sunAMAuthHOTPSMTPUserPassword").isEqualTo(Set.of(CONFIG_PASSWORD));
    }

    @Test
    public void shouldReturnNullPasswordIfNeitherPasswordIsPresent() {
        Map<String, Set<String>> map = mapper.asConfigMap(config, realm);
        assertThat(map).extractingByKey("sunAMAuthHOTPSMTPUserPassword").isEqualTo(Collections.singleton(null));
    }
}
