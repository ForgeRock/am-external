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
 * Copyright 2021-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link PackedVerifier}.
 */
public class PackedVerifierTest {

    private KeyPair ecdsaKeyPair;

    private ListAppender<ILoggingEvent> appender;

    private PackedVerifier verifier;

    @BeforeClass
    public void createKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    @BeforeMethod
    public void setUp() {
        verifier = new PackedVerifier(null, null);

        appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(PackedVerifier.class);
        logger.addAppender(appender);
    }

    @DataProvider
    public Object[][] invalidEcdsaSignatures() {
        return new Object[][]{
                {CoseAlgorithm.ES256, new byte[64]},
                {CoseAlgorithm.ES384, new byte[96]},
                {CoseAlgorithm.ES512, new byte[132]},
        };
    }

    @Test(dataProvider = "invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatures(CoseAlgorithm algorithm, byte[] invalidSignature) {
        // Given
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(algorithm);
        statement.setSig(DerUtils.encodeEcdsaSignature(invalidSignature));
        AttestationObject object = new AttestationObject(verifier, authData, statement);

        // When
        boolean valid = verifier.isSignatureValid(object, new byte[0], ecdsaKeyPair.getPublic());

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    public void shouldFailToVerifyGivenNoCertificates() {
        // Given
        List<X509Certificate> attestationCerts = emptyList();
        AttestedCredentialData credentialData = new AttestedCredentialData(null, 0, null, null, null);
        AuthData authData = new AuthData(null, null, 1, credentialData, new byte[0]);
        AttestationStatement statement = new AttestationStatement();
        statement.setAttestnCerts(attestationCerts);
        AttestationObject attestationObject = new AttestationObject(verifier, authData, statement);

        // When
        VerificationResponse result = verifier.verify(attestationObject, new byte[32]);

        // Then
        assertLoggerMessage("webauthn authentication attestation certificates could not be found");
        assertThat(result.isValid()).isFalse();
    }

    private void assertLoggerMessage(String actualMessage) {
        assertThat(1).isEqualTo(appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertThat(Level.ERROR).isEqualTo(event.getLevel());
        assertThat(actualMessage).isEqualTo(event.getFormattedMessage());
    }

}