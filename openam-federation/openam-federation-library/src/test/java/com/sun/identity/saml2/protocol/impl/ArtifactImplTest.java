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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ArtifactImplTest {

    public static Object[][] xmlCases() {
        return new Object[][] {
                { true, true, "<samlp:Artifact xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" +
                        "AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=</samlp:Artifact>" },
                { true, false,
                        "<samlp:Artifact>AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=</samlp:Artifact>"
                },
                { false, false, "<Artifact>AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=</Artifact>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ArtifactImpl artifact = new ArtifactImpl(ArtifactImpl.ARTIFACT_4_TYPE_CODE, 1, "A".repeat(20),
                "B".repeat(20));

        // When
        String xml = artifact.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
