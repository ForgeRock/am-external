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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.ConnectionFactoryFactory;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Attributes;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.messages.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.secrets.Purpose;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

@RunWith(MockitoJUnitRunner.class)
public class CertificateValidationNodeTest {

    public static final String LDAP_HOST = "localhost:1636";
    public static final char[] BIND_PASSWORD = "password".toCharArray();
    public static final String BIND_DN = "uid=admin";
    @Mock
    private CertificateValidationNode.Config config;
    @Mock
    private ConnectionFactoryFactory connectionFactoryFactory;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private ConnectionEntryReader connectionEntryReader;
    @Mock
    private SearchResultEntry searchResultEntry;
    @Mock
    private Realm realm;

    @Mock
    private Secrets secrets;

    private CertificateValidationNode certificateValidationNode;


    @Before
    public void setup() {
        SystemPropertiesManager.initializeProperties("com.iplanet.am.server.host", "localhost");
        certificateValidationNode = new CertificateValidationNode(config, realm, secrets);
        LDAPUtils.setConnectionFactoryFactory(connectionFactoryFactory);

        given(config.checkCertificateExpiry()).willReturn(false);
        given(config.matchCertificateInLdap()).willReturn(true);
        given(config.certificateLdapServers()).willReturn(Set.of(LDAP_HOST));
        given(config.ldapSearchStartDN()).willReturn(Set.of("ou=identities"));
        given(config.crlHttpParameters()).willReturn(Optional.of("a=b"));
    }

    @Test
    public void testSucceedsWhenConnectingToLdapWithBasicAuth() throws NodeProcessException, LdapException,
                                                                                   SearchResultReferenceIOException,
                                                                                   CertificateEncodingException {
        // given
        X509Certificate certificate = generateCASignedCertificate("uid=user");

        given(config.ldapCertificateAttribute()).willReturn(Optional.of("UID"));
        given(config.userBindDN()).willReturn(Optional.of(BIND_DN));
        given(config.userBindPassword()).willReturn(Optional.of(BIND_PASSWORD));
        given(connectionFactoryFactory.newFailoverConnectionFactory(eq(Set.of(LDAPURL.valueOf("ldap://" + LDAP_HOST))),
                eq(BIND_DN), aryEq(BIND_PASSWORD), anyInt(), any(), anyBoolean(), anyBoolean(), any(), eq(false),
                any(), any(), any()))
                .willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.search(any())).willReturn(connectionEntryReader);
        Iterator<SearchResultEntry> it = List.of(searchResultEntry).iterator();
        given(connectionEntryReader.hasNext()).willAnswer(inv -> it.hasNext());
        given(connectionEntryReader.isEntry()).willReturn(true);
        given(connectionEntryReader.readEntry()).willAnswer(inv -> it.next());
        Attribute certAttribute = Attributes.singletonAttribute("usercertificate",
                ByteString.valueOfBytes(certificate.getEncoded()));
        given(searchResultEntry.getAttribute("usercertificate")).willReturn(certAttribute);

        Action action = certificateValidationNode.process(new TreeContext(
                json(object()),
                json(object(field("X509Certificate", List.of(certificate)))),
                new ExternalRequestContext.Builder().build(),
                emptyList(),
                Optional.empty()));

        assertThat(action.outcome).isEqualTo("TRUE");
    }

    @Test
    public void testFailsIfMtlsEnabledButNoSecretLabelConfigured() {
        // given
        X509Certificate certificate = generateCASignedCertificate("uid=user");

        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificateValidationNode.process(new TreeContext(
                json(object()),
                json(object(field("X509Certificate", List.of(certificate)))),
                new ExternalRequestContext.Builder().build(),
                emptyList(),
                Optional.empty()))).isInstanceOfSatisfying(NodeProcessException.class,
                ex -> {
                    assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
                    assertThat(ex.getCause().getMessage()).isEqualTo("Missing mTLS Secret Label Identifier");
                });
    }

    @Test
    public void testSucceedsWhenMtlsEnabledAndSecretLabelPresent() throws LdapException,
            SearchResultReferenceIOException, NodeProcessException, CertificateEncodingException {
        // given
        X509Certificate certificate = generateCASignedCertificate("uid=user");

        given(config.ldapCertificateAttribute()).willReturn(Optional.of("UID"));
        given(config.sslEnabled()).willReturn(true);
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.of(Purpose.SIGN));
        given(connectionFactoryFactory.newFailoverConnectionFactory(eq(Set.of(LDAPURL.valueOf("ldaps://" + LDAP_HOST))),
                eq(null), eq(null), anyInt(), any(), anyBoolean(), anyBoolean(), any(), eq(true),
                eq(Purpose.SIGN.getLabel()), eq(secrets), eq(realm)))
                .willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.search(any())).willReturn(connectionEntryReader);
        Iterator<SearchResultEntry> it = List.of(searchResultEntry).iterator();
        given(connectionEntryReader.hasNext()).willAnswer(inv -> it.hasNext());
        given(connectionEntryReader.isEntry()).willReturn(true);
        given(connectionEntryReader.readEntry()).willAnswer(inv -> it.next());
        Attribute certAttribute = Attributes.singletonAttribute("usercertificate",
                ByteString.valueOfBytes(certificate.getEncoded()));
        given(searchResultEntry.getAttribute("usercertificate")).willReturn(certAttribute);

        // when
        Action action = certificateValidationNode.process(new TreeContext(
                json(object()),
                json(object(field("X509Certificate", List.of(certificate)))),
                new ExternalRequestContext.Builder().build(),
                emptyList(),
                Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("TRUE");
    }

    private static X509Certificate generateCASignedCertificate(String principalName) {
        KeyPair keyPair = generateKeyPair();
        Date expiryDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
        try {
            X500Principal subjectAndIssuer = new X500Principal(principalName);
            BigInteger serial = new BigInteger(159, new SecureRandom());
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subjectAndIssuer, serial, new Date(), expiryDate,
                    subjectAndIssuer, keyPair.getPublic());
            X509CertificateHolder certHolder =
                    certBuilder.build(new JcaContentSignerBuilder("SHA256WithECDSA").build(keyPair.getPrivate()));

            return new JcaX509CertificateConverter().getCertificate(certHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create CA-signed certificate", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("unable to generate test key-pair", e);
        }
    }
}
