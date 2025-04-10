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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.sae.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.test.bazel.ResourceFinder.findPathToResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SecureAttrsTest {

    private static final String PRIVATE_KEY_ALIAS = "privatekey";
    private static final String PUBLIC_KEY_ALIAS = "defaultkey";
    private static final String PRIVATE_KEY_PASSWORD = "keypass";
    static SecureAttrs symSecureAttrs;
    static SecureAttrs asymSecureAttrs;
    static Map<String, String> attributes;

    @BeforeAll
    static void beforeClass() throws Exception {
        Path keystorePath = findPathToResource(SecureAttrsTest.class, "keystore.jks");

        String password = Files.readAllLines(findPathToResource(SecureAttrsTest.class, "keystorepass")).stream()
                .findFirst().orElseThrow();
        SecureAttrs.dbg = true;
        Properties properties = new Properties();
        properties.setProperty("keystorefile", keystorePath.toAbsolutePath().toString());
        properties.setProperty("keystoretype", "JKS");
        properties.setProperty("keystorepass", password);
        properties.setProperty("privatekeyalias", PRIVATE_KEY_ALIAS);
        properties.setProperty("publickeyalias", PUBLIC_KEY_ALIAS);
        properties.setProperty("privatekeypass", PRIVATE_KEY_PASSWORD);
        properties.setProperty("encryptionkeystrength", "56");
        properties.setProperty("encryptionalgorithm", "DES");
        SecureAttrs.init("testsym", SecureAttrs.SAE_CRYPTO_TYPE_SYM,
                properties);
        SecureAttrs.init("testasym", SecureAttrs.SAE_CRYPTO_TYPE_ASYM,
                properties);

        attributes = Map.of(
                "branch", "bb",
                "mail", "mm",
                "sun.userid", "uu",
                "sun.spappurl", "apapp");

        symSecureAttrs = SecureAttrs.getInstance("testsym");
        asymSecureAttrs = SecureAttrs.getInstance("testasym");
    }

    @Test
    void testEncodeThenDecode() throws Exception {
        String secret = "secret";
        String symEncodedSecret = symSecureAttrs.getEncodedString(attributes, secret);
        System.out.println("Encoded string: " + symEncodedSecret);
        Map<String, String> verificationMap = symSecureAttrs.verifyEncodedString(symEncodedSecret, secret);
        assertThat(verificationMap).isNotNull();

        secret = "privatekey";
        String asymEncodedSecret = asymSecureAttrs.getEncodedString(attributes, secret);
        System.out.println("Encoded string: " + asymEncodedSecret);
        verificationMap = asymSecureAttrs.verifyEncodedString(asymEncodedSecret, secret);
        assertThat(verificationMap).isNotNull();
    }

    @Test
    void testDecodeWithIncorrectSecrets() throws Exception {
        String secret = "secret";
        String encodedSecret = symSecureAttrs.getEncodedString(attributes, secret);
        Map<String, String> verificationMap = symSecureAttrs.verifyEncodedString(encodedSecret, "junk");
        assertThat(verificationMap).isNull();

        secret = "privatekey";
        String secondEncodedSecret = asymSecureAttrs.getEncodedString(attributes, secret);
        verificationMap = asymSecureAttrs.verifyEncodedString(secondEncodedSecret, "junk");
        assertThat(verificationMap).isNull();
    }

    @Test
    void testDecodeWithCorrectSecret() throws Exception {
        String secret = "secret";
        String encodedSecret = symSecureAttrs.getEncodedString(attributes, secret);
        Map<String, String> verificationMap = symSecureAttrs.verifyEncodedString(encodedSecret, secret);
        assertThat(verificationMap).isNotNull();

        secret = "privatekey";
        String secondEncodedSecret = asymSecureAttrs.getEncodedString(attributes, secret);
        verificationMap = asymSecureAttrs.verifyEncodedString(secondEncodedSecret, secret);
        assertThat(verificationMap).isNotNull();
    }

    @Test
    void testEncodeSigned() throws Exception {
        String secret = "privatekey";
        String secondEncodedSecret = symSecureAttrs.getEncodedString(attributes, secret, secret);
        Map<String, String> verificationMap = symSecureAttrs.verifyEncodedString(secondEncodedSecret, secret, secret);
        assertThat(verificationMap).isNotNull();

        secret = "secret";
        String encodedSecret = symSecureAttrs.getEncodedString(attributes, secret, secret);
        verificationMap = symSecureAttrs.verifyEncodedString(encodedSecret, secret, secret);
        assertThat(verificationMap).isNotNull();
    }

}
