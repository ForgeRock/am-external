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
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * A series of utility functions for working with files from a test point of view.
 */
public class FileUtils {

    private FileUtils() {
        // Utility class, no instances required.
    }

    /**
     * Create a temporary file.
     * <p>
     * This file should be deleted at the end of testing. The references created by this utility are
     * located in the system temporary location. If they are not cleaned up automatically by the test
     * then they should be cleaned up by the operating system at some point.
     * </p>
     *
     * @return A non null, empty file location that is guaranteed to exist.
     */
    public static File temporaryFile() {
        try {
            File temp = File.createTempFile(FileUtils.class.getSimpleName(), ".tmp");
            assertThat(temp).exists();
            return temp;
        } catch (IOException e) {
            throw new AssertionError("failed to create temporary file", e);
        }
    }

    /**
     * Get an input stream from the classpath.
     *
     * @param name the class path
     * @return the input stream
     */
    public static InputStream getFromClasspath(String name) {
        return FileUtils.class.getResourceAsStream("/" + name);
    }

    /**
     * Copy the contents of a class path into a temporary file.
     *
     * @param classPath the class path
     * @return the temporary file
     */
    public static File copyClassPathDataToTempFile(String classPath) {
        return FileUtils.copy(FileUtils.getFromClasspath(classPath));
    }

    /**
     * Copy the given {@link InputStream} to a new temporary file.
     * <p>
     * <b>Note</b>: The caller of this function is responsible for cleaning up the file created
     * after the test.
     * </p>
     *
     * @param stream Stream which contains the data to copy into the file.
     * @return {@link File} which contains the data.
     */
    public static File copy(InputStream stream) {
        return copy(stream, temporaryFile());
    }

    /**
     * Copy the given {@link InputStream} replacing a current file
     * <p>
     * <b>Note</b>: The caller of this function is responsible for cleaning up the file created
     * after the test.
     * </p>
     *
     * @param stream Stream which contains the data to copy into the file
     * @param path   the path of the file replace
     * @return {@link File} which contains the data.
     */
    public static File copy(InputStream stream, File path) {
        try {
            CopyOption option = StandardCopyOption.REPLACE_EXISTING;
            Files.copy(stream, path.toPath(), option);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return path;
    }

    /**
     * Read the contents of an {@link InputStream} which is assumed to contain UTF-8 encoded
     * test and convert it to a String.
     *
     * @param stream Non null, possibly empty.
     * @return Non null, possibly empty.
     */
    public static String readStream(InputStream stream) {
        try (DataInputStream din = new DataInputStream(stream)) {
            int available = din.available();
            byte[] buffer = new byte[available]; // Assumption that works for test purposes.
            din.readFully(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
