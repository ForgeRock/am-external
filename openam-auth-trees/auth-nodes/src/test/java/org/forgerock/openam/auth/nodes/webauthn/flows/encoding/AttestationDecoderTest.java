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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;

import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

public class AttestationDecoderTest {

    @Mock
    IdentityUtils mockIdUtils;
    @Mock
    TrustAnchorValidator.Factory factory;
    @Mock
    TrustAnchorUtilities trustUtilities;

    private AttestationDecoder decoder;

    @BeforeTest
    public void theSetUp() {
        initMocks(this);
        decoder = new AttestationDecoder(trustUtilities, factory, new AuthDataDecoder(),
                new FlowUtilities(mockIdUtils), new NoneVerifier());
    }

    @DataProvider(name = "formatVerifier")
    public Object[][] formatVerifier() {
        return new Object[][] {
                { "none", NoneVerifier.class },
                { "fido-u2f", FidoU2fVerifier.class },
                { "packed", PackedVerifier.class },
                { "tpm", TpmVerifier.class }
        };
    }

    @DataProvider(name = "unsupportedFormats")
    public Object[][] unsupportedFormats() {
        return new Object[][] {
                { "android-key" },
                { "android-safetynet" }
        };
    }

    @Test(dataProvider = "unsupportedFormats", expectedExceptions = DecodingException.class)
    public void testExceptionThrownForUnsupportedFormats(String name) throws Exception {
        //given
        byte[] input = generateCborFormatBytes(name);

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        //expecting an exception
    }

    @Test(dataProvider = "formatVerifier")
    public void testCorrectVerifierReturnedForAttestationFormat(String name,
                                                                Class<AttestationVerifier> verifier) throws Exception {
        //given
        byte[] input = generateCborFormatBytes(name);

        //when
        AttestationObject result = decoder.decode(input, null, null, false);

        //then
        assertThat(result.attestationVerifier).isOfAnyClassIn(verifier);
    }

    private byte[] generateCborFormatBytes(String name) throws CborException {
        CborBuilder cborBuilder = new CborBuilder();
        cborBuilder.addMap().put("fmt", name);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder cborEncoder = new CborEncoder(baos);
        cborEncoder.encode(cborBuilder.build());

        return baos.toByteArray();
    }

}
