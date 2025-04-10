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
package org.forgerock.openam.auth.nodes.webauthn.metadata.utils;

import static java.text.MessageFormat.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

/**
 * Resolves a resource to the contents of the input stream.
 * <p>
 * ClassPath or {@link URL} or File locations are supported.
 */
public class ResourceResolver {
    /**
     * Resolve the provided location to determine whether it is a {@link File} or a
     * {@link URL} location.
     * <p>
     * <b>Note:</b> The caller is responsible for closing this stream.
     * </p>
     *
     * @param location Non-null location to test
     * @return a possibly empty {@link InputStream}
     * @throws IOException If there was an unexpected error whilst opening the location
     */
    public InputStream resolve(String location) throws IOException {
        if (isUrl(location)) {
            URL url = getUrl(location).orElseThrow(() -> new IOException(
                    format("Location was not a URL: {0}", location)));
            return resolve(url);
        } else if (new File(location).exists()) {
            return new FileInputStream(location);
        } else {
            throw new IOException("Location did not exist: " + location);
        }
    }

    /**
     * Resolve the location of the {@link URL} and provide an open
     * {@link InputStream} to the resolved location.
     * <p>
     * <b>Note</b>: The caller is responsible for closing this stream.
     * </p>
     *
     * @param url a non-null {@link URL}
     * @return an opened {@link InputStream}
     * @throws IOException If there was an error opening the {@link InputStream}
     * @see #resolve(String)
     */
    public InputStream resolve(URL url) throws IOException {
        return url.openStream();
    }

    /**
     * Resolve the location and in addition read the entire stream contents
     * to provide to the caller.
     *
     * @param location a possible null or empty location
     * @return a non-null {@link byte} array of the contents of that stream
     * @throws IOException if there was any error whilst attempting
     *                     to read the stream
     */
    public byte[] resolveAsBytes(String location) throws IOException {
        try (InputStream stream = resolve(location)) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            stream.transferTo(bout);
            return bout.toByteArray();
        }
    }

    /**
     * Utility function to generate a {@link URL} for a given {@link String} value.
     *
     * @param urlLocation possibly null or empty {@link String}
     * @return {@link Optional} which will contain the URL if the {@link String} was
     * a valid URL, otherwise it will be empty
     */
    public static Optional<URL> getUrl(String urlLocation) {
        try {
            return Optional.of(new URL(urlLocation));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    private boolean isUrl(String location) {
        return location.startsWith("http://") || location.startsWith("https://");
    }
}
