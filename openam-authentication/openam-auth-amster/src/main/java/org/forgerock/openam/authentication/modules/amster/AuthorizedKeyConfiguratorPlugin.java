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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.amster;

import static com.sun.identity.shared.encode.Base64.encode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.forgerock.openam.utils.file.FileUtils.createFileIfMissing;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

import jakarta.servlet.ServletContext;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.forgerock.openam.homedirectory.AMHomeDirectoryService;
import org.forgerock.openam.homedirectory.HomeDirectoryUtils;

import com.iplanet.sso.SSOToken;
import com.sun.identity.setup.ConfiguratorPlugin;
import com.sun.identity.setup.SetupConstants;

/**
 * A configurator plugin to create a local authorized key for Amster.
 */
public class AuthorizedKeyConfiguratorPlugin implements ConfiguratorPlugin {

    private static final String RSA_ALGORITHM_ID = "ssh-rsa";
    private static final String KEY_FILE = "amster_rsa";

    @Override
    public void reinitConfiguratioFile(String baseDir) {
        // no-op
    }

    @Override
    public void doPostConfiguration(ServletContext servletCtx, SSOToken adminSSOToken) {
        createLocalAmsterKey(true);
    }

    /**
     * Write an RSA private key and create an authorized_keys file in the provided directory.
     *
     * @param authorizeKey Whether to add the public key to the {@code authorized_keys} file.
     */
    public static void createLocalAmsterKey(boolean authorizeKey) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            KeyPair keyPair = generator.generateKeyPair();
            String directory = AMHomeDirectoryService.getHomeDirectory().getAmsterKeysDir();
            HomeDirectoryUtils.createMissingDirectory(directory);
            writePrivateKey(directory, keyPair);
            writePublicKey(directory, keyPair, authorizeKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write Amster keys", e);
        }
    }

    private static void writePrivateKey(String directory, KeyPair keyPair) throws IOException {
        Path fileWithPermissions = createFileIfMissing(Path.of(directory, KEY_FILE), OWNER_READ, OWNER_WRITE);
        File keyFile = fileWithPermissions.toFile();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(new FileOutputStream(keyFile)))) {
            pemWriter.writeObject(keyPair.getPrivate());
        }
    }

    private static void writePublicKey(String directory, KeyPair keyPair, boolean authorizeKey) throws IOException {
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        ByteArrayOutputStream publicKey = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(publicKey);
        write(dos, RSA_ALGORITHM_ID.getBytes(US_ASCII));
        write(dos, key.getPublicExponent().toByteArray());
        write(dos, key.getModulus().toByteArray());
        dos.close();
        String keyLine = RSA_ALGORITHM_ID + " " + encode(publicKey.toByteArray())
                + " Local Amster Key\n";
        writePublicKeyFile(directory, keyLine, KEY_FILE + ".pub");
        writePublicKeyFile(directory, authorizeKey ? keyLine : "", SetupConstants.AUTHORIZED_KEYS_FILENAME);
    }

    private static void writePublicKeyFile(String directory, String keyLine, String file) throws IOException {
        Path fileWithPermissions = createFileIfMissing(Path.of(directory, file), OWNER_READ, OWNER_WRITE);
        File authorizedKeys = fileWithPermissions.toFile();
        Files.writeString(authorizedKeys.toPath(), keyLine, US_ASCII, authorizedKeys.exists() ? APPEND : CREATE);
    }

    private static void write(DataOutputStream dos, byte[] bytes) throws IOException {
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }
}
