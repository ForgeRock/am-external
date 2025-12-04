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

import java.util.Date;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;

public class ArtifactResolveImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:ArtifactResolve xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Consent=\"blah\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "Version=\"2.0\"" +
                        ">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Artifact>" +
                        "AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=" +
                        "</samlp:Artifact>" +
                        "</samlp:ArtifactResolve>" },
                { true, false, "<samlp:ArtifactResolve " +
                        "Consent=\"blah\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "Version=\"2.0\"" +
                        ">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Artifact>" +
                        "AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=" +
                        "</samlp:Artifact></samlp:ArtifactResolve>" },
                { false, false, "<ArtifactResolve " +
                        "Consent=\"blah\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "Version=\"2.0\"" +
                        ">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Artifact>AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=</Artifact>" +
                        "</ArtifactResolve>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ArtifactResolveImpl artifactResolve = new ArtifactResolveImpl();
        artifactResolve.setID("testID");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        artifactResolve.setIssuer(issuer);
        artifactResolve.setIssueInstant(new Date(1622626177000L));
        artifactResolve.setDestination("https://test.example.com/");
        artifactResolve.setConsent("blah");
        artifactResolve.setVersion("2.0");
        ArtifactImpl artifact = new ArtifactImpl("AAQAAUFBQUFBQUFBQUFBQUFBQUFBQUFBQkJCQkJCQkJCQkJCQkJCQkJCQkI=");
        artifactResolve.setArtifact(artifact);

        // When
        String xml = artifactResolve.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
