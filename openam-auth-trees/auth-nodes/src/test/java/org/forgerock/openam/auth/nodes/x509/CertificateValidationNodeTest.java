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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.x509.CertificateValidationNode.CertificateValidationOutcome.TRUE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.forgerock.am.identity.application.IdentityStoreLdapParametersProvider;
import org.forgerock.am.identity.application.model.LdapConnectionParameters;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.secrets.Purpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.security.cert.AMCRLStore;
import com.sun.identity.security.cert.AMCertPath;
import com.sun.identity.security.cert.AMCertStore;
import com.sun.identity.sm.SMSException;

@ExtendWith(MockitoExtension.class)
public class CertificateValidationNodeTest {

    public static final String LDAP_HOST = "localhost:1636";
    public static final char[] BIND_PASSWORD = "password".toCharArray();
    public static final String BIND_DN = "uid=admin";

    @Mock
    private CertificateValidationNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private Secrets secrets;
    @Mock
    private IdentityStoreLdapParametersProvider identityStoreLdapParametersProvider;

    @InjectMocks
    private CertificateValidationNode certificateValidationNode;

    @Test
    void shouldValidateCertificateGivenServerFromNodeConfig() throws NodeProcessException {
        // Given
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.ldapCertificateAttribute()).willReturn(Optional.of("UID"));
        given(config.certificateLdapServers()).willReturn(Set.of(LDAP_HOST));
        given(config.ldapSearchStartDN()).willReturn(Set.of("ou=identities"));
        given(config.userBindDN()).willReturn(Optional.of(BIND_DN));
        given(config.userBindPassword()).willReturn(Optional.of(BIND_PASSWORD));
        X509Certificate certificate = generateCertificate();
        LdapConnectionParameters ldapParameters = LdapConnectionParameters.builder()
                .ldapServers(LDAPUtils.getLdapUrls("localhost", 1636, false))
                .ldapUser(BIND_DN)
                .ldapPassword(BIND_PASSWORD)
                .startSearchLocation("ou=identities")
                .realm(realm)
                .build();
        try (MockedConstruction<AMCertStore> ignored = mockCertificateInCertStore(certificate, ldapParameters)) {

            // When
            Action action = certificateValidationNode.process(treeContextWithCertificate(certificate));

            // Then
            assertThat(action.outcome).isEqualTo(TRUE.name());
        }
    }

    @Test
    void shouldFailToValidateCertificateGivenServerFromNodeConfigWithMtlsEnabledButNoSecretLabelConfigured() {
        // Given
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.certificateLdapServers()).willReturn(Set.of(LDAP_HOST));
        given(config.ldapSearchStartDN()).willReturn(Set.of("ou=identities"));
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.empty());

        // When - Then
        assertThatThrownBy(() -> certificateValidationNode.process(treeContextWithCertificate(generateCertificate())))
                .isInstanceOf(NodeProcessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Missing mTLS Secret Label Identifier");
    }

    @Test
    void shouldValidateCertificateGivenServerFromNodeConfigWhenMtlsEnabledAndSecretLabelPresent()
            throws NodeProcessException {
        // Given
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.ldapCertificateAttribute()).willReturn(Optional.of("UID"));
        given(config.certificateLdapServers()).willReturn(Set.of(LDAP_HOST));
        given(config.ldapSearchStartDN()).willReturn(Set.of("ou=identities"));
        given(config.sslEnabled()).willReturn(true);
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.of(Purpose.SIGN));
        X509Certificate certificate = generateCertificate();
        LdapConnectionParameters ldapParameters = LdapConnectionParameters.builder()
                .ldapServers(LDAPUtils.getLdapUrls("localhost", 1636, true))
                .startSearchLocation("ou=identities")
                .mtlsEnabled(true)
                .mtlsSecretLabel(Purpose.SIGN.getLabel())
                .realm(realm)
                .build();
        try (MockedConstruction<AMCertStore> ignored = mockCertificateInCertStore(certificate, ldapParameters)) {

            // When
            Action action = certificateValidationNode.process(treeContextWithCertificate(certificate));

            // Then
            assertThat(action.outcome).isEqualTo(TRUE.name());
        }
    }

    @Test
    void shouldValidateCertificateGivenIdentityStoreSelected() throws NodeProcessException, SMSException {
        // Given
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.ldapCertificateAttribute()).willReturn(Optional.of("UID"));
        given(config.certificateIdentityStore()).willReturn("realmIdentityStore");
        LdapConnectionParameters ldapParameters = LdapConnectionParameters.builder()
                .ldapServers(LDAPUtils.getLdapUrls("localhost", 1636, false))
                .ldapUser(BIND_DN)
                .ldapPassword(BIND_PASSWORD)
                .startSearchLocation("ou=identities")
                .build();
        given(identityStoreLdapParametersProvider.getLdapParams(realm, "realmIdentityStore"))
                .willReturn(ldapParameters);
        X509Certificate certificate = generateCertificate();
        try (MockedConstruction<AMCertStore> ignored = mockCertificateInCertStore(certificate, ldapParameters)) {

            // When
            Action action = certificateValidationNode.process(treeContextWithCertificate(certificate));

            // Then
            assertThat(action.outcome).isEqualTo(TRUE.name());
        }
    }

    @Test
    void shouldFailToValidateCertificateGivenSelectedIdentityStoreCannotBeRetrieved() throws SMSException {
        // Given
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.certificateIdentityStore()).willReturn("realmIdentityStore");
        given(identityStoreLdapParametersProvider.getLdapParams(realm, "realmIdentityStore"))
                .willThrow(SMSException.class);

        // When - Then
        assertThatThrownBy(() -> certificateValidationNode.process(treeContextWithCertificate(generateCertificate())))
                .isInstanceOf(NodeProcessException.class).hasMessage("Unable to set LDAP Server configuration");
    }

    @Test
    void shouldValidateCertificateGivenCertificateNotRevokedInCrl() throws SMSException, NodeProcessException {
        // Given
        given(config.matchCertificateToCRL()).willReturn(true);
        given(config.crlMatchingCertificateAttribute()).willReturn("UID");
        given(config.certificateIdentityStore()).willReturn("realmIdentityStore");
        LdapConnectionParameters ldapParameters = LdapConnectionParameters.builder()
                .ldapServers(LDAPUtils.getLdapUrls("localhost", 1636, false))
                .ldapUser(BIND_DN)
                .ldapPassword(BIND_PASSWORD)
                .startSearchLocation("ou=identities")
                .build();
        given(identityStoreLdapParametersProvider.getLdapParams(realm, "realmIdentityStore"))
                .willReturn(ldapParameters);
        X509Certificate certificate = generateCertificate();
        X509CRL crl = mock(X509CRL.class);
        try (MockedConstruction<AMCertPath> ignoredCertPath = mockConstruction(AMCertPath.class, (mock, context) -> {
            Vector<?> crls = (Vector<?>) context.arguments().get(0);
            if (crls == null) {
                given(mock.verify(new X509Certificate[]{certificate}, false, false)).willReturn(true);
            } else if (crls.contains(crl)) {
                given(mock.verify(new X509Certificate[]{certificate}, true, false)).willReturn(true);
            }
        });
             MockedConstruction<AMCRLStore> ignoredCrlStore = mockConstruction(AMCRLStore.class, (mock, context) ->
                     given(mock.getCRL(certificate, false, false, null)).willReturn(crl))) {

            // When
            Action action = certificateValidationNode.process(treeContextWithCertificate(certificate));

            // Then
            assertThat(action.outcome).isEqualTo(TRUE.name());
        }
    }

    private X509Certificate generateCertificate() {
        KeyPair keyPair = generateKeyPair();
        Date expiryDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
        try {
            X500Principal subjectAndIssuer = new X500Principal("UID=user");
            BigInteger serial = new BigInteger(159, new SecureRandom());
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subjectAndIssuer, serial, new Date(), expiryDate,
                    subjectAndIssuer, keyPair.getPublic());
            X509CertificateHolder certHolder =
                    certBuilder.build(new JcaContentSignerBuilder("SHA256WithECDSA").build(keyPair.getPrivate()));

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create certificate", e);
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("unable to generate test key-pair", e);
        }
    }

    private TreeContext treeContextWithCertificate(X509Certificate certificate) {
        return new TreeContext(
                json(object()),
                json(object(field("X509Certificate", List.of(certificate)))),
                new ExternalRequestContext.Builder().build(),
                emptyList(),
                Optional.empty());
    }

    private MockedConstruction<AMCertStore> mockCertificateInCertStore(X509Certificate certificate,
            LdapConnectionParameters ldapParameters) {
        return mockConstruction(AMCertStore.class, (mock, context) -> {
            if (context.arguments().get(0).equals(ldapParameters)) {
                given(mock.getCertificate()).willReturn(certificate);
            }
        });
    }
}
