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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;


import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.Test;

import com.sun.identity.shared.encode.Base64;

public class JwtHeaderUtilitiesTest {

    @Test
    public void getHeaderTest() {
        // Given
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = Base64.encode(header.getBytes(StandardCharsets.UTF_8));
        String jwtString = encodedHeader + "." + "payload";

        // When
        Optional<String> actual = JwtHeaderUtilities.getHeader("alg", jwtString);

        // Then
        assertThat("HS256").isEqualTo(actual.get());
    }
}