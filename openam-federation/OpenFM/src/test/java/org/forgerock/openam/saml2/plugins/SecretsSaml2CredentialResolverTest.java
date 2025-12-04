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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.secrets.DefaultingPurpose;
import org.forgerock.openam.secrets.SecretInitialisationException;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Secret;
import org.forgerock.secrets.keys.KeyDecryptionKey;
import org.forgerock.secrets.keys.KeyEncryptionKey;
import org.forgerock.secrets.keys.KeyFormatRaw;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.common.SAML2Utils;

@ExtendWith(MockitoExtension.class)
public class SecretsSaml2CredentialResolverTest {

    @Mock
    private Secrets secrets;
    @Mock
    private KeyStoreSaml2CredentialResolver keyStoreResolver;
    @Mock
    private SecretsProviderFacade realmSecrets;
    @Mock
    private Promise<Secret, NoSuchSecretException> activeSecret;
    @Mock
    private SigningKey signingKey;
    @Mock
    private PrivateKey privateKey;
    @Mock
    private KeyEncryptionKey encryptionKey;
    @Mock
    private KeyDecryptionKey decryptionKey;
    @Mock
    private X509Certificate certificate;
    @Captor
    private ArgumentCaptor<DefaultingPurpose<SigningKey>> purposeArgumentCaptor;

    private RealmTestHelper realmTestHelper;
    private MockedStatic<SAML2Utils> utils;
    private Saml2CredentialResolver resolver;

    @BeforeEach
    void setup() {
        utils = mockStatic(SAML2Utils.class);

        resolver = new SecretsSaml2CredentialResolver(secrets, keyStoreResolver);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
    }

    @AfterEach
    void tearDown() {
        realmTestHelper.tearDownRealmClass();
        utils.close();
    }

    @Test
    void testGetEntityRoleSigningDetailsForIDP() throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");

        given(secrets.getRealmSecrets(any())).willReturn(realmSecrets);
        given(signingKey.export(KeyFormatRaw.INSTANCE)).willReturn(privateKey);
        given(signingKey.getCertificate(X509Certificate.class)).willReturn(Optional.of(certificate));
        given(realmSecrets.getActiveSecret(any(), any())).willReturn(activeSecret);
        given(activeSecret.getOrThrowIfInterrupted()).willReturn(signingKey);

        // When
        Saml2SigningCredentials credentials = resolver.resolveActiveSigningCredential("/", "idpEntityID", IDP);

        // Then
        verify(realmSecrets).getActiveSecret(purposeArgumentCaptor.capture(), any());
        assertThat(purposeArgumentCaptor.getValue().getDefaultPurpose().getLabel())
                .isEqualTo("am.default.applications.federation.entity.providers.saml2.idp.signing");
        assertThat(credentials.getSigningKey()).isEqualTo(privateKey);
        assertThat(credentials.getSigningCertificate()).isEqualTo(certificate);
    }

    @Test
    void testGetEntityRoleSigningDetailsForSP() throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");

        given(secrets.getRealmSecrets(any())).willReturn(realmSecrets);
        given(signingKey.export(KeyFormatRaw.INSTANCE)).willReturn(privateKey);
        given(signingKey.getCertificate(X509Certificate.class)).willReturn(Optional.of(certificate));
        given(realmSecrets.getActiveSecret(any(), any())).willReturn(activeSecret);
        given(activeSecret.getOrThrowIfInterrupted()).willReturn(signingKey);

        // When
        Saml2SigningCredentials credentials = resolver.resolveActiveSigningCredential("/", "spEntityID", SP);

        // Then
        verify(realmSecrets).getActiveSecret(purposeArgumentCaptor.capture(), any());
        assertThat(purposeArgumentCaptor.getValue().getDefaultPurpose().getLabel())
                .isEqualTo("am.default.applications.federation.entity.providers.saml2.sp.signing");
        assertThat(credentials.getSigningKey()).isEqualTo(privateKey);
        assertThat(credentials.getSigningCertificate()).isEqualTo(certificate);
    }

    @Test
    void testGetEntityRoleSigningDetailsReturnsNullOnNoSecretFound() throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");

        given(secrets.getRealmSecrets(any())).willReturn(realmSecrets);
        given(realmSecrets.getActiveSecret(any(), any())).willReturn(activeSecret);
        given(activeSecret.getOrThrowIfInterrupted()).willReturn(signingKey);

        // Given
        given(activeSecret.getOrThrowIfInterrupted()).willThrow(NoSuchSecretException.class);

        // When
        Saml2SigningCredentials credentials =
                resolver.resolveActiveSigningCredential("/", "spEntityID", SP);

        // Then
        assertThat(credentials.getSigningKey()).isNull();
        assertThat(credentials.getSigningCertificate()).isNull();
    }

    @Test
    void shouldReturnValidEncryptionCertificates() throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");

        given(secrets.getRealmSecrets(any())).willReturn(realmSecrets);
        given(realmSecrets.getValidSecrets(any(), any()))
                .willReturn(Promises.newResultPromise(Stream.of(decryptionKey)));
        given(decryptionKey.getCertificate(any())).willReturn(Optional.of(certificate));

        // When
        Set<X509Certificate> certs = resolver.resolveValidEncryptionCredentials("/", "entityId", IDP);

        // Then
        assertThat(certs).containsOnly(certificate);
    }

    @Test
    void shouldReturnEmptySetOfEncryptionCertificatesWhenRealmSecretsThrowExceptionAndSecretIDIdentifierIsNull()
            throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn(null);
        given(secrets.getRealmSecrets(any())).willThrow(SecretInitialisationException.class);

        // When
        Set<X509Certificate> certs = resolver.resolveValidEncryptionCredentials("/", "entityId", IDP);

        // Then
        assertThat(certs).isEmpty();
    }

    @Test
    void shouldReturnValidDecryptionCertificates() throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn("secret");

        given(secrets.getRealmSecrets(any())).willReturn(realmSecrets);
        given(realmSecrets.getValidSecrets(any(), any()))
                .willReturn(Promises.newResultPromise(Stream.of(decryptionKey)));
        given(decryptionKey.export(KeyFormatRaw.INSTANCE)).willReturn(privateKey);

        // When
        Set<PrivateKey> certs = resolver.resolveValidDecryptionCredentials("/", "entityId", SP);

        // Then
        assertThat(certs).containsOnly(privateKey);
    }

    @Test
    void shouldReturnEmptySetOfDecryptionCertificatesWhenRealmSecretsThrowExceptionAndSecretIDIdentifierIsNull()
            throws Exception {
        // Given
        utils.when(
                ()->SAML2Utils.getAttributeValueFromSSOConfig(
                        any(String.class),any(String.class),any(String.class),any(String.class)
                )
        ).thenReturn(null);
        given(secrets.getRealmSecrets(any())).willThrow(SecretInitialisationException.class);

        // When
        Set<PrivateKey> certs = resolver.resolveValidDecryptionCredentials("/", "entityId", SP);

        // Then
        assertThat(certs).isEmpty();
    }
}
