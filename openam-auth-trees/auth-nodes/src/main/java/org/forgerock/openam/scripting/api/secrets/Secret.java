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
 * Copyright 2020-2022 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api.secrets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.util.Reject;

/**
 * Value object that models a secret as a value. This can be represented to the caller in either
 * {@link StandardCharsets#UTF_8} or in the underlying {@code byte[]} form depending on what the
 * secret is. The caller is responsible for determining how to interpret the contained data.
 */
@SupportedAll
public class Secret {

    private final String utf8;
    private final byte[] bytes;

    /**
     * Default constructor exposing required dependencies.
     * @param genericSecret Non null.
     */
    public Secret(@Nonnull GenericSecret genericSecret) {
        Reject.ifNull(genericSecret);
        utf8 = genericSecret.revealAsUtf8(String::new);
        bytes = genericSecret.reveal(bytes -> Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Provide a {@link StandardCharsets#UTF_8} rendering of the {@link GenericSecret}.
     * @return Non null String representation of the secret.
     */
    public String getAsUtf8() {
        return utf8;
    }

    /**
     * Access the secret value in {@link byte[]} form.
     * @return The {@code byte[]} representation of the secret.
     */
    public byte[] getAsBytes() {
        return bytes;
    }
}
