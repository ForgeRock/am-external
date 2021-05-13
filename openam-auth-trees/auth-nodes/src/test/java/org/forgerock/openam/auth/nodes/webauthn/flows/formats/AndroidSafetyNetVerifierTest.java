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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.encode.Base64;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

public class AndroidSafetyNetVerifierTest {

    @Mock
    IdentityUtils mockIdUtils;
    @Mock
    TrustAnchorValidator.Factory factory;
    @Mock
    TrustAnchorUtilities trustUtilities;

    private AttestationDecoder decoder;
    private Logger logger;

    @BeforeMethod
    public void theSetUp() {
        initMocks(this);
        decoder = new AttestationDecoder(trustUtilities, factory, new AuthDataDecoder(),
                new FlowUtilities(mockIdUtils), new NoneVerifier());
        logger = mock(Logger.class);
    }

    @DataProvider
    public static Object[][] data() throws CborException {
        return new Object[][]{
                {"Missing Version", getDeviceAttestationDataMissingVersion(), getEmulatorClientDataHash(), false},
                {"Failed to parse the attestation statement response as a valid JWS",
                        getDeviceAttestationDataInvalidJWS(), getEmulatorClientDataHash(), false},
                {"Failed to verify JWS signature", getDeviceAttestationDataDummyJWS(),
                        getEmulatorClientDataHash(), true},
                {"Android SafetyNet attestation - The response is not identical to the Base64 "
                        + "encoding of the SHA-256 hash of the concatenation of authenticatorData and clientDataHash.",
                        getRealDeviceAttestationData(), getDeviceClientDataHashWithInvalidData(), false},
                {"Android SafetyNet attestation - ctsProfileMatch is False", getEmulatorAttestationData(),
                        getEmulatorClientDataHash(), false},
                {null, getRealDeviceAttestationData(), getRealDeviceClientDataHash(), false},
        };
    }


    @Test(dataProvider = "data")
    public void test(String expectLogMessage, byte[] attestationData, byte[] clientDataHash, boolean verifySignature)
            throws DecodingException, CertificateException, NoSuchAlgorithmException {
        AttestationObject attestationObject = decoder.decode(attestationData, null,
                null, false);
        assertThat(attestationObject.attestationVerifier).isInstanceOf(AndroidSafetyNetVerifier.class);

        AndroidSafetyNetVerifier androidSafetyNetVerifier = new AndroidSafetyNetVerifier(logger);

        if (!verifySignature) {
            androidSafetyNetVerifier = Mockito.spy(androidSafetyNetVerifier);
            doReturn(generateCertificate()).when(androidSafetyNetVerifier).verifySignature(any());
        }

        VerificationResponse response = androidSafetyNetVerifier.verify(attestationObject, clientDataHash);
        assertThat(response.getAttestationType()).isEqualTo(AttestationType.BASIC);

        if (expectLogMessage != null) {
            ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
            verify(logger, times(1)).error(stringCaptor.capture(), (Throwable) any());
            assertThat(stringCaptor.getValue()).isEqualTo(expectLogMessage);
            assertThat(response.isValid()).isFalse();
        } else {
            assertThat(response.isValid()).isTrue();
            assertThat(response.getCertificate()).isInstanceOf(X509Certificate.class);
        }
    }

    @Test
    public void testInvalidHost() throws DecodingException, CertificateException, NoSuchAlgorithmException {
        byte[] attestationData = getRealDeviceAttestationData();
        byte[] clientDataHash = getRealDeviceClientDataHash();
        AttestationObject attestationObject = decoder.decode(attestationData, null,
                null, false);
        assertThat(attestationObject.attestationVerifier).isInstanceOf(AndroidSafetyNetVerifier.class);

        AndroidSafetyNetVerifier androidSafetyNetVerifier = new AndroidSafetyNetVerifier(logger);

        androidSafetyNetVerifier = Mockito.spy(androidSafetyNetVerifier);
        doReturn(generateCertificate("CN=dummy")).when(androidSafetyNetVerifier).verifySignature(any());

        VerificationResponse response = androidSafetyNetVerifier.verify(attestationObject, clientDataHash);
        assertThat(response.getAttestationType()).isEqualTo(AttestationType.BASIC);

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, times(1)).error(stringCaptor.capture(), (Throwable) any());
        assertThat(stringCaptor.getValue()).isEqualTo("Host verification failed");
        assertThat(response.isValid()).isFalse();
    }

    //Android Emulator generated attestation Data
    private static byte[] getEmulatorAttestationData() {
        return Base64.decode(
                "o2NmbXRxYW5kcm9pZC1zYWZldHluZXRnYXR0U3RtdKJjdmVyaTIwNDUxNjA0OWhyZXNwb25zZVkU4WV5SmhiR2N"
                        + "pT2lKU1V6STFOaUlzSW5nMVl5STZXeUpOU1VsR2EzcERRMEpJZFdkQmQwbENRV2RKVWtGT1kxTnJhbVJ6Tlc0Mks"
                        + "wTkJRVUZCUVVGd1lUQmpkMFJSV1VwTGIxcEphSFpqVGtGUlJVeENVVUYzVVdwRlRFMUJhMGRCTVZWRlFtaE5RMVpX"
                        + "VFhoSWFrRmpRbWRPVmtKQmIxUkdWV1IyWWpKa2MxcFRRbFZqYmxaNlpFTkNWRnBZU2pKaFYwNXNZM3BGVkUxQ1JVZ"
                        + "EJNVlZGUVhoTlMxSXhVbFJKUlU1Q1NVUkdVRTFVUVdWR2R6QjVUVVJCZUUxVVRYaE5WRkY0VGtSc1lVWjNNSGxOVk"
                        + "VGNFRWUkZlRTFVVVhoT1JHeGhUVWQzZUVONlFVcENaMDVXUWtGWlZFRnNWbFJOVWsxM1JWRlpSRlpSVVVsRmQzQkV"
                        + "XVmQ0Y0ZwdE9YbGliV3hvVFZKWmQwWkJXVVJXVVZGSVJYY3hUbUl6Vm5Wa1IwWndZbWxDVjJGWFZqTk5VazEzUlZG"
                        + "WlJGWlJVVXRGZDNCSVlqSTVibUpIVldkVVJYaEVUVkp6ZDBkUldVUldVVkZFUlhoS2FHUklVbXhqTTFGMVdWYzFhM"
                        + "k50T1hCYVF6VnFZakl3ZDJkblJXbE5RVEJIUTFOeFIxTkpZak5FVVVWQ1FWRlZRVUUwU1VKRWQwRjNaMmRGUzBGdl"
                        + "NVSkJVVU5YUlhKQ1VWUkhXa2RPTVdsYVlrNDVaV2hTWjJsbVYwSjRjV2t5VUdSbmVIY3dNMUEzVkhsS1dtWk5lR3B"
                        + "3TlV3M2FqRkhUbVZRU3pWSWVtUnlWVzlKWkRGNVEwbDVRazE1ZUhGbllYcHhaM1J3V0RWWGNITllWelJXWmsxb1Nt"
                        + "Sk9NVmt3T1hGNmNYQTJTa1FyTWxCYVpHOVVWVEZyUmxKQlRWZG1UQzlWZFZwMGF6ZHdiVkpZWjBkdE5XcExSSEphT"
                        + "1U1NFpUQTBkazFaVVhJNE9FNXhkMWN2YTJaYU1XZFVUMDVKVlZRd1YzTk1WQzgwTlRJeVFsSlhlR1ozZUdNelVVVX"
                        + "hLMVJMVjJ0TVEzSjJaV3MyVjJ4SmNYbGhRelV5VnpkTlJGSTRUWEJHWldKNWJWTkxWSFozWmsxU2QzbExVVXhVTUR"
                        + "OVlREUjJkRFE0ZVVWak9ITndOM2RVUVVoTkwxZEVaemhSYjNSaGNtWTRUMEpJYTI1dldqa3lXR2wyYVdGV05uUlJj"
                        + "V2hTVDBoRFptZHRia05ZYVhobVZ6QjNSVmhEZG5GcFRGUmlVWFJWWWt4elV5ODRTVkowWkZocmNGRkNPVUZuVFVKQ"
                        + "lFVZHFaMmRLV1UxSlNVTldSRUZQUW1kT1ZraFJPRUpCWmpoRlFrRk5RMEpoUVhkRmQxbEVWbEl3YkVKQmQzZERaMW"
                        + "xKUzNkWlFrSlJWVWhCZDBWM1JFRlpSRlpTTUZSQlVVZ3ZRa0ZKZDBGRVFXUkNaMDVXU0ZFMFJVWm5VVlUyUkVoQ2Q"
                        + "zTkJkbUkxTTJjdlF6QTNjSEpVZG5aM1RsRlJURmwzU0hkWlJGWlNNR3BDUW1kM1JtOUJWVzFPU0RSaWFFUnllalYy"
                        + "YzFsS09GbHJRblZuTmpNd1NpOVRjM2RhUVZsSlMzZFpRa0pSVlVoQlVVVkZWMFJDVjAxRFkwZERRM05IUVZGVlJrS"
                        + "jZRVUpvYUhSdlpFaFNkMDlwT0haaU1rNTZZME0xZDJFeWEzVmFNamwyV25rNWJtUklUWGhpZWtWM1MzZFpTVXQzV1"
                        + "VKQ1VWVklUVUZMUjBneWFEQmtTRUUyVEhrNWQyRXlhM1ZhTWpsMlduazVibU16U1hsTU1HUlZWWHBHVUUxVE5XcGp"
                        + "ibEYzU0ZGWlJGWlNNRkpDUWxsM1JrbEpVMWxZVWpCYVdFNHdURzFHZFZwSVNuWmhWMUYxV1RJNWRFMURSVWRCTVZW"
                        + "a1NVRlJZVTFDWjNkRFFWbEhXalJGVFVGUlNVTk5RWGRIUTJselIwRlJVVUl4Ym10RFFsRk5kMHgzV1VSV1VqQm1Ra"
                        + "05uZDBwcVFXdHZRMHRuU1VsWlpXRklVakJqUkc5MlRESk9lV0pETlhkaE1tdDFXakk1ZGxwNU9VaFdSazE0VkhwRm"
                        + "RWa3pTbk5OU1VsQ1FrRlpTMHQzV1VKQ1FVaFhaVkZKUlVGblUwSTVVVk5DT0dkRWQwRklZMEU1YkhsVlREbEdNMDF"
                        + "EU1ZWV1FtZEpUVXBTVjJwMVRrNUZlR3Q2ZGprNFRVeDVRVXg2UlRkNFdrOU5RVUZCUm5adWRYa3dXbmRCUVVKQlRV"
                        + "RlRSRUpIUVdsRlFUZGxMekJaVW5VemQwRkdiVmRJTWpkTk1uWmlWbU5hTDIxeWNDczBjbVpaWXk4MVNWQktNamxHT"
                        + "m1kRFNWRkRia3REUTBGaFkxWk9aVmxhT0VORFpsbGtSM0JDTWtkelNIaDFUVTlJYTJFdlR6UXhhbGRsUml0NlowSX"
                        + "hRVVZUVlZwVE5uYzNjeloyZUVWQlNESkxhaXRMVFVSaE5XOUxLekpOYzNoMFZDOVVUVFZoTVhSdlIyOUJRVUZDWWp"
                        + "VM2MzUktUVUZCUVZGRVFVVlpkMUpCU1dkRldHSnBiMUJpU25BNWNVTXdSR295TlRoRVJrZFRVazFCVlN0YVFqRkZh"
                        + "VlpGWW1KaUx6UlZkazVGUTBsQ2FFaHJRblF4T0haU2JqbDZSSFo1Y21aNGVYVmtZMGhVVDFOc00yZFVZVmxCTHpkN"
                        + "VZDOUNhVWcwVFVFd1IwTlRjVWRUU1dJelJGRkZRa04zVlVGQk5FbENRVkZFU1VGalVVSnNiV1E0VFVWblRHUnljbk"
                        + "pOWWtKVVEzWndUVmh6ZERVcmQzZ3lSR3htWVdwS1RrcFZVRFJxV1VacVdWVlJPVUl6V0RSRk1ucG1ORGx1V0ROQmV"
                        + "YVmFSbmhCY1U5U2JtSnFMelZxYTFrM1lUaHhUVW93YWpFNWVrWlBRaXR4WlhKNFpXTXdibWh0T0dkWmJFeGlVVzAy"
                        + "YzB0Wk4xQXdaWGhtY2pkSWRVc3pUV3RRTVhCbFl6RTBkMFpGVldGSGNVUjNWV0pIWjJ3dmIybDZNemhHV0VORkswT"
                        + "lhPRVV4VVVGRlZXWjJZbEZRVkZsaVMzaFphaXQwUTA1c2MzTXdZbFJUYjB3eVdqSmtMMm96UW5CTU0wMUdkekI1ZU"
                        + "ZOTEwxVlVjWGxyVEhJeVFTOU5aR2hLVVcxNGFTdEhLMDFMVWxOelVYSTJNa0Z1V21GMU9YRTJXVVp2YVNzNVFVVkl"
                        + "LMEUwT0ZoMFNYbHphRXg1UTFSVk0waDBLMkZMYjJoSGJuaEJOWFZzTVZoU2JYRndPRWgyWTBGME16bFFPVFZHV2tk"
                        + "R1NtVXdkWFpzZVdwUGQwRjZXSFZOZFRkTksxQlhVbU1pTENKTlNVbEZVMnBEUTBGNlMyZEJkMGxDUVdkSlRrRmxUe"
                        + "kJ0Y1VkT2FYRnRRa3BYYkZGMVJFRk9RbWRyY1docmFVYzVkekJDUVZGelJrRkVRazFOVTBGM1NHZFpSRlpSVVV4Rm"
                        + "VHUklZa2M1YVZsWGVGUmhWMlIxU1VaS2RtSXpVV2RSTUVWblRGTkNVMDFxUlZSTlFrVkhRVEZWUlVOb1RVdFNNbmg"
                        + "yV1cxR2MxVXliRzVpYWtWVVRVSkZSMEV4VlVWQmVFMUxVako0ZGxsdFJuTlZNbXh1WW1wQlpVWjNNSGhPZWtFeVRW"
                        + "UlZkMDFFUVhkT1JFcGhSbmN3ZVUxVVJYbE5WRlYzVFVSQmQwNUVTbUZOUlVsNFEzcEJTa0puVGxaQ1FWbFVRV3hXV"
                        + "kUxU05IZElRVmxFVmxGUlMwVjRWa2hpTWpsdVlrZFZaMVpJU2pGak0xRm5WVEpXZVdSdGJHcGFXRTE0UlhwQlVrSm"
                        + "5UbFpDUVUxVVEydGtWVlY1UWtSUlUwRjRWSHBGZDJkblJXbE5RVEJIUTFOeFIxTkpZak5FVVVWQ1FWRlZRVUUwU1V"
                        + "KRWQwRjNaMmRGUzBGdlNVSkJVVVJSUjAwNVJqRkpkazR3TlhwclVVODVLM1JPTVhCSlVuWktlbnA1VDFSSVZ6VkVl"
                        + "a1ZhYUVReVpWQkRiblpWUVRCUmF6STRSbWRKUTJaTGNVTTVSV3R6UXpSVU1tWlhRbGxyTDJwRFprTXpVak5XV2sxa"
                        + "1V5OWtUalJhUzBORlVGcFNja0Y2UkhOcFMxVkVlbEp5YlVKQ1NqVjNkV1JuZW01a1NVMVpZMHhsTDFKSFIwWnNOWG"
                        + "xQUkVsTFoycEZkaTlUU2tndlZVd3JaRVZoYkhST01URkNiWE5MSzJWUmJVMUdLeXRCWTNoSFRtaHlOVGx4VFM4NWF"
                        + "XdzNNVWt5WkU0NFJrZG1ZMlJrZDNWaFpXbzBZbGhvY0RCTVkxRkNZbXA0VFdOSk4wcFFNR0ZOTTFRMFNTdEVjMkY0"
                        + "YlV0R2MySnFlbUZVVGtNNWRYcHdSbXhuVDBsbk4zSlNNalY0YjNsdVZYaDJPSFpPYld0eE4zcGtVRWRJV0d0NFYxa"
                        + "zNiMGM1YWl0S2ExSjVRa0ZDYXpkWWNrcG1iM1ZqUWxwRmNVWktTbE5RYXpkWVFUQk1TMWN3V1RONk5XOTZNa1F3WX"
                        + "pGMFNrdDNTRUZuVFVKQlFVZHFaMmRGZWsxSlNVSk1la0ZQUW1kT1ZraFJPRUpCWmpoRlFrRk5RMEZaV1hkSVVWbEV"
                        + "WbEl3YkVKQ1dYZEdRVmxKUzNkWlFrSlJWVWhCZDBWSFEwTnpSMEZSVlVaQ2QwMURUVUpKUjBFeFZXUkZkMFZDTDNk"
                        + "UlNVMUJXVUpCWmpoRFFWRkJkMGhSV1VSV1VqQlBRa0paUlVaS2FsSXJSelJSTmpncllqZEhRMlpIU2tGaWIwOTBPV"
                        + "U5tTUhKTlFqaEhRVEZWWkVsM1VWbE5RbUZCUmtwMmFVSXhaRzVJUWpkQllXZGlaVmRpVTJGTVpDOWpSMWxaZFUxRV"
                        + "ZVZERRM05IUVZGVlJrSjNSVUpDUTJ0M1NucEJiRUpuWjNKQ1owVkdRbEZqZDBGWldWcGhTRkl3WTBSdmRrd3lPV3B"
                        + "qTTBGMVkwZDBjRXh0WkhaaU1tTjJXak5PZVUxcVFYbENaMDVXU0ZJNFJVdDZRWEJOUTJWblNtRkJhbWhwUm05a1NG"
                        + "SjNUMms0ZGxrelNuTk1ia0p5WVZNMWJtSXlPVzVNTW1SNlkycEpkbG96VG5sTmFUVnFZMjEzZDFCM1dVUldVakJuU"
                        + "WtSbmQwNXFRVEJDWjFwdVoxRjNRa0ZuU1hkTGFrRnZRbWRuY2tKblJVWkNVV05EUVZKWlkyRklVakJqU0UwMlRIaz"
                        + "VkMkV5YTNWYU1qbDJXbms1ZVZwWVFuWmpNbXd3WWpOS05VeDZRVTVDWjJ0eGFHdHBSemwzTUVKQlVYTkdRVUZQUTB"
                        + "GUlJVRkhiMEVyVG01dU56aDVObkJTYW1RNVdHeFJWMDVoTjBoVVoybGFMM0l6VWs1SGEyMVZiVmxJVUZGeE5sTmpk"
                        + "R2s1VUVWaGFuWjNVbFF5YVZkVVNGRnlNREptWlhOeFQzRkNXVEpGVkZWM1oxcFJLMnhzZEc5T1JuWm9jMDg1ZEhaQ"
                        + "1EwOUpZWHB3YzNkWFF6bGhTamw0YW5VMGRGZEVVVWc0VGxaVk5sbGFXaTlZZEdWRVUwZFZPVmw2U25GUWFsazRjVE"
                        + "5OUkhoeWVtMXhaWEJDUTJZMWJ6aHRkeTkzU2pSaE1rYzJlSHBWY2paR1lqWlVPRTFqUkU4eU1sQk1Va3cyZFROTk5"
                        + "GUjZjek5CTWsweGFqWmllV3RLV1drNGQxZEpVbVJCZGt0TVYxcDFMMkY0UWxaaWVsbHRjVzEzYTIwMWVreFRSRmMx"
                        + "YmtsQlNtSkZURU5SUTFwM1RVZzFOblF5UkhaeGIyWjRjelpDUW1ORFJrbGFWVk53ZUhVMmVEWjBaREJXTjFOMlNrT"
                        + "kRiM05wY2xOdFNXRjBhaTg1WkZOVFZrUlJhV0psZERoeEx6ZFZTelIyTkZwVlRqZ3dZWFJ1V25veGVXYzlQU0pkZl"
                        + "EuZXlKdWIyNWpaU0k2SWxkRFltaEZRbXhIYm5CbU1UaEtORlYzWVZGWlV6SkNkaXR4V21abkwyVkRVRXBKVVdKNWF"
                        + "FczNWemc5SWl3aWRHbHRaWE4wWVcxd1RYTWlPakUyTURZNU5EVTJNVGc0TXpnc0ltRndhMUJoWTJ0aFoyVk9ZVzFs"
                        + "SWpvaVkyOXRMbWR2YjJkc1pTNWhibVJ5YjJsa0xtZHRjeUlzSW1Gd2EwUnBaMlZ6ZEZOb1lUSTFOaUk2SWxGcVVuS"
                        + "k5TVVZSUXpZNVJESmpRbmRMUXpWbU1EWjJlSGQ0UzFZclVucEpWRnBSZDBObE5HaEVTamc5SWl3aVkzUnpVSEp2Wm"
                        + "1sc1pVMWhkR05vSWpwbVlXeHpaU3dpWVhCclEyVnlkR2xtYVdOaGRHVkVhV2RsYzNSVGFHRXlOVFlpT2xzaU9GQXh"
                        + "jMWN3UlZCS1kzTnNkemRWZWxKemFWaE1OalIzSzA4MU1FVmtLMUpDU1VOMFlYa3haekkwVFQwaVhTd2lZbUZ6YVdO"
                        + "SmJuUmxaM0pwZEhraU9tWmhiSE5sTENKbGRtRnNkV0YwYVc5dVZIbHdaU0k2SWtKQlUwbERJbjAuRnJ6dTlTeHlGV"
                        + "2pCWEh0WVlXTDQ1TWJ6VDd0aVk5MEZvQzRGVEhCRTZHUUl6ZTR3SUxrbDhneEdKV3cwYTRwR3dvSUktal9XY3Q3RU"
                        + "hGYmJKUjhFd0QxSzJGQ1h2VlByN1YzTU1WbmxlSlVnZ0Y2YTFmcmVqTk9pdElaLWhMZDNkdXdNeWVOeDBVZk11NGJ"
                        + "DVlF6T0R4OHdiS19OWlhoak0zUFlvUzdyX3RLRUJPS0phVTdhSU9SUHFlLUR3ZXhHWFA4aHNhZ3BfUlVoZEdxMjRR"
                        + "dUpWdVp0amJZaDZTN2lWWkxmbUppYWFWU0xGbWdTWEdrRkJxSW5CZkRmSGlzdVltX08tRXI4TFZBRzVpVm1RX2JQS"
                        + "2RGNnJUZFIxcmNwX1NVUXZQN3pySGstZEFHUk5XTjlKbjJjcG40ckE0R3U5TV9ORTczOXFfcWhBc2VTRVRfd0dBaG"
                        + "F1dGhEYXRhWMV9iBOwo65y3ID60flOUUhOk+WwtdCVk9RNv7tFPe2eIUUAAAAAuT/ZYfLmRi+xIoIAIkfeeABBAVu"
                        + "coVshvsU1E/Mkz9Hr3fzw4nXqssvW1KxFELT8H0tKafaSQSI4JiiTyL91dGZ4E5NbMEbcnmLZL7xnNm7YpwWlAQID"
                        + "JiABIVggvmtkr9fRLKQdiydFtuPfgOBfRwgQ0RfMkbjfCnO7FZgiWCCCjV7f0hCKRznffR/wYmcuPM3yCGoIRmBhg"
                        + "j8k8ybIxg==");
    }

    private static byte[] getEmulatorClientDataHash() {
        return EncodingUtilities.getHash("{\"type\":\"webauthn.create\",\"challenge\":"
                + "\"qRT9T_-a47Aij575tqZH5X5xOq2yvLzeuBko9EGhrfM\",\"origin\":"
                + "\"android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw\",\"androidPackageName\":"
                + "\"org.forgerock.auth\"}");
    }

    //Real device generated attestation Data
    private static byte[] getRealDeviceAttestationData() {
        return Base64.decode(
                "o2NmbXRxYW5kcm9pZC1zYWZldHluZXRnYXR0U3RtdKJjdmVyaTIwNDcxMzAzN2hyZXNwb25zZVkU9GV5SmhiR"
                        + "2NpT2lKU1V6STFOaUlzSW5nMVl5STZXeUpOU1VsR2EzcERRMEpJZFdkQmQwbENRV2RKVWtGT1kxTnJhbVJ6Tlc"
                        + "0MkswTkJRVUZCUVVGd1lUQmpkMFJSV1VwTGIxcEphSFpqVGtGUlJVeENVVUYzVVdwRlRFMUJhMGRCTVZWRlFta"
                        + "E5RMVpXVFhoSWFrRmpRbWRPVmtKQmIxUkdWV1IyWWpKa2MxcFRRbFZqYmxaNlpFTkNWRnBZU2pKaFYwNXNZM3B"
                        + "GVkUxQ1JVZEJNVlZGUVhoTlMxSXhVbFJKUlU1Q1NVUkdVRTFVUVdWR2R6QjVUVVJCZUUxVVRYaE5WRkY0VGtSc"
                        + "1lVWjNNSGxOVkVGNFRWUkZlRTFVVVhoT1JHeGhUVWQzZUVONlFVcENaMDVXUWtGWlZFRnNWbFJOVWsxM1JWRlp"
                        + "SRlpSVVVsRmQzQkVXVmQ0Y0ZwdE9YbGliV3hvVFZKWmQwWkJXVVJXVVZGSVJYY3hUbUl6Vm5Wa1IwWndZbWxDV"
                        + "jJGWFZqTk5VazEzUlZGWlJGWlJVVXRGZDNCSVlqSTVibUpIVldkVVJYaEVUVkp6ZDBkUldVUldVVkZFUlhoS2F"
                        + "HUklVbXhqTTFGMVdWYzFhMk50T1hCYVF6VnFZakl3ZDJkblJXbE5RVEJIUTFOeFIxTkpZak5FVVVWQ1FWRlZRV"
                        + "UUwU1VKRWQwRjNaMmRGUzBGdlNVSkJVVU5YUlhKQ1VWUkhXa2RPTVdsYVlrNDVaV2hTWjJsbVYwSjRjV2t5VUd"
                        + "SbmVIY3dNMUEzVkhsS1dtWk5lR3B3TlV3M2FqRkhUbVZRU3pWSWVtUnlWVzlKWkRGNVEwbDVRazE1ZUhGbllYc"
                        + "HhaM1J3V0RWWGNITllWelJXWmsxb1NtSk9NVmt3T1hGNmNYQTJTa1FyTWxCYVpHOVVWVEZyUmxKQlRWZG1UQzl"
                        + "WZFZwMGF6ZHdiVkpZWjBkdE5XcExSSEphT1U1NFpUQTBkazFaVVhJNE9FNXhkMWN2YTJaYU1XZFVUMDVKVlZRd"
                        + "1YzTk1WQzgwTlRJeVFsSlhlR1ozZUdNelVVVXhLMVJMVjJ0TVEzSjJaV3MyVjJ4SmNYbGhRelV5VnpkTlJGSTR"
                        + "UWEJHWldKNWJWTkxWSFozWmsxU2QzbExVVXhVTUROVlREUjJkRFE0ZVVWak9ITndOM2RVUVVoTkwxZEVaemhSY"
                        + "jNSaGNtWTRUMEpJYTI1dldqa3lXR2wyYVdGV05uUlJjV2hTVDBoRFptZHRia05ZYVhobVZ6QjNSVmhEZG5GcFR"
                        + "GUmlVWFJWWWt4elV5ODRTVkowWkZocmNGRkNPVUZuVFVKQlFVZHFaMmRLV1UxSlNVTldSRUZQUW1kT1ZraFJPR"
                        + "UpCWmpoRlFrRk5RMEpoUVhkRmQxbEVWbEl3YkVKQmQzZERaMWxKUzNkWlFrSlJWVWhCZDBWM1JFRlpSRlpTTUZ"
                        + "SQlVVZ3ZRa0ZKZDBGRVFXUkNaMDVXU0ZFMFJVWm5VVlUyUkVoQ2QzTkJkbUkxTTJjdlF6QTNjSEpVZG5aM1RsR"
                        + "lJURmwzU0hkWlJGWlNNR3BDUW1kM1JtOUJWVzFPU0RSaWFFUnllalYyYzFsS09GbHJRblZuTmpNd1NpOVRjM2R"
                        + "hUVZsSlMzZFpRa0pSVlVoQlVVVkZWMFJDVjAxRFkwZERRM05IUVZGVlJrSjZRVUpvYUhSdlpFaFNkMDlwT0haa"
                        + "U1rNTZZME0xZDJFeWEzVmFNamwyV25rNWJtUklUWGhpZWtWM1MzZFpTVXQzV1VKQ1VWVklUVUZMUjBneWFEQmt"
                        + "TRUUyVEhrNWQyRXlhM1ZhTWpsMlduazVibU16U1hsTU1HUlZWWHBHVUUxVE5XcGpibEYzU0ZGWlJGWlNNRkpDU"
                        + "WxsM1JrbEpVMWxZVWpCYVdFNHdURzFHZFZwSVNuWmhWMUYxV1RJNWRFMURSVWRCTVZWa1NVRlJZVTFDWjNkRFF"
                        + "WbEhXalJGVFVGUlNVTk5RWGRIUTJselIwRlJVVUl4Ym10RFFsRk5kMHgzV1VSV1VqQm1Ra05uZDBwcVFXdHZRM"
                        + "HRuU1VsWlpXRklVakJqUkc5MlRESk9lV0pETlhkaE1tdDFXakk1ZGxwNU9VaFdSazE0VkhwRmRWa3pTbk5OU1V"
                        + "sQ1FrRlpTMHQzV1VKQ1FVaFhaVkZKUlVGblUwSTVVVk5DT0dkRWQwRklZMEU1YkhsVlREbEdNMDFEU1ZWV1FtZ"
                        + "EpUVXBTVjJwMVRrNUZlR3Q2ZGprNFRVeDVRVXg2UlRkNFdrOU5RVUZCUm5adWRYa3dXbmRCUVVKQlRVRlRSRUp"
                        + "IUVdsRlFUZGxMekJaVW5VemQwRkdiVmRJTWpkTk1uWmlWbU5hTDIxeWNDczBjbVpaWXk4MVNWQktNamxHTm1kR"
                        + "FNWRkRia3REUTBGaFkxWk9aVmxhT0VORFpsbGtSM0JDTWtkelNIaDFUVTlJYTJFdlR6UXhhbGRsUml0NlowSXh"
                        + "RVVZUVlZwVE5uYzNjeloyZUVWQlNESkxhaXRMVFVSaE5XOUxLekpOYzNoMFZDOVVUVFZoTVhSdlIyOUJRVUZDW"
                        + "WpVM2MzUktUVUZCUVZGRVFVVlpkMUpCU1dkRldHSnBiMUJpU25BNWNVTXdSR295TlRoRVJrZFRVazFCVlN0YVF"
                        + "qRkZhVlpGWW1KaUx6UlZkazVGUTBsQ2FFaHJRblF4T0haU2JqbDZSSFo1Y21aNGVYVmtZMGhVVDFOc00yZFVZV"
                        + "mxCTHpkNVZDOUNhVWcwVFVFd1IwTlRjVWRUU1dJelJGRkZRa04zVlVGQk5FbENRVkZFU1VGalVVSnNiV1E0VFV"
                        + "WblRHUnljbkpOWWtKVVEzWndUVmh6ZERVcmQzZ3lSR3htWVdwS1RrcFZVRFJxV1VacVdWVlJPVUl6V0RSRk1uc"
                        + "G1ORGx1V0ROQmVYVmFSbmhCY1U5U2JtSnFMelZxYTFrM1lUaHhUVW93YWpFNWVrWlBRaXR4WlhKNFpXTXdibWh"
                        + "0T0dkWmJFeGlVVzAyYzB0Wk4xQXdaWGhtY2pkSWRVc3pUV3RRTVhCbFl6RTBkMFpGVldGSGNVUjNWV0pIWjJ3d"
                        + "mIybDZNemhHV0VORkswTlhPRVV4VVVGRlZXWjJZbEZRVkZsaVMzaFphaXQwUTA1c2MzTXdZbFJUYjB3eVdqSmt"
                        + "MMm96UW5CTU0wMUdkekI1ZUZOTEwxVlVjWGxyVEhJeVFTOU5aR2hLVVcxNGFTdEhLMDFMVWxOelVYSTJNa0Z1V"
                        + "21GMU9YRTJXVVp2YVNzNVFVVklLMEUwT0ZoMFNYbHphRXg1UTFSVk0waDBLMkZMYjJoSGJuaEJOWFZzTVZoU2J"
                        + "YRndPRWgyWTBGME16bFFPVFZHV2tkR1NtVXdkWFpzZVdwUGQwRjZXSFZOZFRkTksxQlhVbU1pTENKTlNVbEZVM"
                        + "nBEUTBGNlMyZEJkMGxDUVdkSlRrRmxUekJ0Y1VkT2FYRnRRa3BYYkZGMVJFRk9RbWRyY1docmFVYzVkekJDUVZ"
                        + "GelJrRkVRazFOVTBGM1NHZFpSRlpSVVV4RmVHUklZa2M1YVZsWGVGUmhWMlIxU1VaS2RtSXpVV2RSTUVWblRGT"
                        + "kNVMDFxUlZSTlFrVkhRVEZWUlVOb1RVdFNNbmgyV1cxR2MxVXliRzVpYWtWVVRVSkZSMEV4VlVWQmVFMUxVako"
                        + "0ZGxsdFJuTlZNbXh1WW1wQlpVWjNNSGhPZWtFeVRWUlZkMDFFUVhkT1JFcGhSbmN3ZVUxVVJYbE5WRlYzVFVSQ"
                        + "mQwNUVTbUZOUlVsNFEzcEJTa0puVGxaQ1FWbFVRV3hXVkUxU05IZElRVmxFVmxGUlMwVjRWa2hpTWpsdVlrZFZ"
                        + "aMVpJU2pGak0xRm5WVEpXZVdSdGJHcGFXRTE0UlhwQlVrSm5UbFpDUVUxVVEydGtWVlY1UWtSUlUwRjRWSHBGZ"
                        + "DJkblJXbE5RVEJIUTFOeFIxTkpZak5FVVVWQ1FWRlZRVUUwU1VKRWQwRjNaMmRGUzBGdlNVSkJVVVJSUjAwNVJ"
                        + "qRkpkazR3TlhwclVVODVLM1JPTVhCSlVuWktlbnA1VDFSSVZ6VkVla1ZhYUVReVpWQkRiblpWUVRCUmF6STRSb"
                        + "WRKUTJaTGNVTTVSV3R6UXpSVU1tWlhRbGxyTDJwRFprTXpVak5XV2sxa1V5OWtUalJhUzBORlVGcFNja0Y2Ukh"
                        + "OcFMxVkVlbEp5YlVKQ1NqVjNkV1JuZW01a1NVMVpZMHhsTDFKSFIwWnNOWGxQUkVsTFoycEZkaTlUU2tndlZVd"
                        + "3JaRVZoYkhST01URkNiWE5MSzJWUmJVMUdLeXRCWTNoSFRtaHlOVGx4VFM4NWFXdzNNVWt5WkU0NFJrZG1ZMlJ"
                        + "rZDNWaFpXbzBZbGhvY0RCTVkxRkNZbXA0VFdOSk4wcFFNR0ZOTTFRMFNTdEVjMkY0YlV0R2MySnFlbUZVVGtNN"
                        + "WRYcHdSbXhuVDBsbk4zSlNNalY0YjNsdVZYaDJPSFpPYld0eE4zcGtVRWRJV0d0NFYxazNiMGM1YWl0S2ExSjV"
                        + "Ra0ZDYXpkWWNrcG1iM1ZqUWxwRmNVWktTbE5RYXpkWVFUQk1TMWN3V1RONk5XOTZNa1F3WXpGMFNrdDNTRUZuV"
                        + "FVKQlFVZHFaMmRGZWsxSlNVSk1la0ZQUW1kT1ZraFJPRUpCWmpoRlFrRk5RMEZaV1hkSVVWbEVWbEl3YkVKQ1d"
                        + "YZEdRVmxKUzNkWlFrSlJWVWhCZDBWSFEwTnpSMEZSVlVaQ2QwMURUVUpKUjBFeFZXUkZkMFZDTDNkUlNVMUJXV"
                        + "UpCWmpoRFFWRkJkMGhSV1VSV1VqQlBRa0paUlVaS2FsSXJSelJSTmpncllqZEhRMlpIU2tGaWIwOTBPVU5tTUh"
                        + "KTlFqaEhRVEZWWkVsM1VWbE5RbUZCUmtwMmFVSXhaRzVJUWpkQllXZGlaVmRpVTJGTVpDOWpSMWxaZFUxRVZVZ"
                        + "ERRM05IUVZGVlJrSjNSVUpDUTJ0M1NucEJiRUpuWjNKQ1owVkdRbEZqZDBGWldWcGhTRkl3WTBSdmRrd3lPV3B"
                        + "qTTBGMVkwZDBjRXh0WkhaaU1tTjJXak5PZVUxcVFYbENaMDVXU0ZJNFJVdDZRWEJOUTJWblNtRkJhbWhwUm05a"
                        + "1NGSjNUMms0ZGxrelNuTk1ia0p5WVZNMWJtSXlPVzVNTW1SNlkycEpkbG96VG5sTmFUVnFZMjEzZDFCM1dVUld"
                        + "VakJuUWtSbmQwNXFRVEJDWjFwdVoxRjNRa0ZuU1hkTGFrRnZRbWRuY2tKblJVWkNVV05EUVZKWlkyRklVakJqU"
                        + "0UwMlRIazVkMkV5YTNWYU1qbDJXbms1ZVZwWVFuWmpNbXd3WWpOS05VeDZRVTVDWjJ0eGFHdHBSemwzTUVKQlV"
                        + "YTkdRVUZQUTBGUlJVRkhiMEVyVG01dU56aDVObkJTYW1RNVdHeFJWMDVoTjBoVVoybGFMM0l6VWs1SGEyMVZiV"
                        + "mxJVUZGeE5sTmpkR2s1VUVWaGFuWjNVbFF5YVZkVVNGRnlNREptWlhOeFQzRkNXVEpGVkZWM1oxcFJLMnhzZEc"
                        + "5T1JuWm9jMDg1ZEhaQ1EwOUpZWHB3YzNkWFF6bGhTamw0YW5VMGRGZEVVVWc0VGxaVk5sbGFXaTlZZEdWRVUwZ"
                        + "FZPVmw2U25GUWFsazRjVE5OUkhoeWVtMXhaWEJDUTJZMWJ6aHRkeTkzU2pSaE1rYzJlSHBWY2paR1lqWlVPRTF"
                        + "qUkU4eU1sQk1Va3cyZFROTk5GUjZjek5CTWsweGFqWmllV3RLV1drNGQxZEpVbVJCZGt0TVYxcDFMMkY0UWxaa"
                        + "WVsbHRjVzEzYTIwMWVreFRSRmMxYmtsQlNtSkZURU5SUTFwM1RVZzFOblF5UkhaeGIyWjRjelpDUW1ORFJrbGF"
                        + "WVk53ZUhVMmVEWjBaREJXTjFOMlNrTkRiM05wY2xOdFNXRjBhaTg1WkZOVFZrUlJhV0psZERoeEx6ZFZTelIyT"
                        + "kZwVlRqZ3dZWFJ1V25veGVXYzlQU0pkZlEuZXlKdWIyNWpaU0k2SW5Sdk9URnRNVEZuVDJnNE5HWjNMMnRsWlc"
                        + "4MGIySlhOa3cyUVRocGRrSlVRalZxVGsxWGIzRnFZa2s5SWl3aWRHbHRaWE4wWVcxd1RYTWlPakUyTURZNU5Ea"
                        + "zFOakV5TVRRc0ltRndhMUJoWTJ0aFoyVk9ZVzFsSWpvaVkyOXRMbWR2YjJkc1pTNWhibVJ5YjJsa0xtZHRjeUl"
                        + "zSW1Gd2EwUnBaMlZ6ZEZOb1lUSTFOaUk2SW1rdmJXSlJPVzVOVGs5NVZrVTBkSEpVZWxWVmExUlhia1ZVZHl0U"
                        + "05FbEpaVzkzTW5SNVNVZEdVMUU5SWl3aVkzUnpVSEp2Wm1sc1pVMWhkR05vSWpwMGNuVmxMQ0poY0d0RFpYSjB"
                        + "hV1pwWTJGMFpVUnBaMlZ6ZEZOb1lUSTFOaUk2V3lJNFVERnpWekJGVUVwamMyeDNOMVY2VW5OcFdFdzJOSGNyV"
                        + "HpVd1JXUXJVa0pKUTNSaGVURm5NalJOUFNKZExDSmlZWE5wWTBsdWRHVm5jbWwwZVNJNmRISjFaU3dpWlhaaGJ"
                        + "IVmhkR2x2YmxSNWNHVWlPaUpDUVZOSlF5eElRVkpFVjBGU1JWOUNRVU5MUlVRaWZRLmNQVXBDSnRmRktja3RFbk"
                        + "NPUHFoSU1WcWhoNlF1VVpUamx5bmpBSXQzazJLbkMwclVELTRkYTN0ZU1TOE1WanVUUHJRNVF2a2tzZmlOUVoyS"
                        + "1FXaHV2dzN6NjBmeEVCZFd3amxFY29hbmJNeHhuaWhoUGZNYXI0WHdlZkxYTklLNEVrTHRkNjJhRWlNWlFlb25"
                        + "3MkFROXhnZW5vc0RrQ3pGODRKN2pzUkFGOVBnbEo2cmFmMEpGMWdvSmRGc0dBSHItM0w5UDd3bjFSeEpTLUYxaF"
                        + "JmbEdmbXU4UWdnYXQtQkxCWG0xRDJhSll6RjQzQmFNTUptLV9LS1pycEtSOGQ2NVpWa21yQ3RhX2szTGVNZE4zb"
                        + "2E5SFZ6WElZN1lWdDNZTWZFd0I4b1NjM2NYV0ZpTnh1NTBHcm9IaXFpbWswXzFWUVRNeGFqYS01N0pLQnRzbkh1"
                        + "d2hhdXRoRGF0YVjFfYgTsKOuctyA+tH5TlFITpPlsLXQlZPUTb+7RT3tniFFAAAAALk/2WHy5kYvsSKCACJH3ng"
                        + "AQQHMYFxP7wZDo2aORvptreK1mGL2oAVoWQUPTU0uGGa6p1dLQEcHjW6JbVTMg9YMGtZo9q15Vio/zdGYF3J/GW"
                        + "uZpQECAyYgASFYIDwypxzVhrAZdPw8FzXB+BxulbggWarlJmxydtosbO/tIlggZblIWtuXtwzRAWysHWTX3D6Q/h/"
                        + "vFiUCoNQ/zJ7tT8U=");
    }

    private static byte[] getRealDeviceClientDataHash() {
        return EncodingUtilities.getHash("{\"type\":\"webauthn.create\",\"challenge\":"
                + "\"_JsE1ROMX1oMa5fN9ssrIuwAdkDHuG4z4_EAb4U6SIY\",\"origin\":"
                + "\"android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw\",\"androidPackageName\":"
                + "\"org.forgerock.auth\"}");
    }

    //Real device generated attestation Data
    private static byte[] getDeviceClientDataHashWithInvalidData() {
        return EncodingUtilities.getHash("{\"type\":\"webauthn.create\",\"challenge\":\"dummy\",\"origin\":"
                + "\"android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw\",\"androidPackageName\":"
                + "\"org.forgerock.auth\"}");
    }

    //Invalid JWS
    private static byte[] getDeviceAttestationDataInvalidJWS() throws CborException {
        CborBuilder cborBuilder = new CborBuilder();
        cborBuilder.addMap().put("fmt", "android-safetynet")
                .putMap("attStmt").put("ver", "1234").put("response", new byte[]{1, 2, 3});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder cborEncoder = new CborEncoder(baos);
        cborEncoder.encode(cborBuilder.build());

        return baos.toByteArray();
    }

    private static Object getDeviceAttestationDataMissingVersion() throws CborException {
        CborBuilder cborBuilder = new CborBuilder();
        cborBuilder.addMap().put("fmt", "android-safetynet")
                .putMap("attStmt").put("response", new byte[]{1, 2, 3});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder cborEncoder = new CborEncoder(baos);
        cborEncoder.encode(cborBuilder.build());

        return baos.toByteArray();
    }

    private static Object getDeviceAttestationDataDummyJWS() throws CborException {
        CborBuilder cborBuilder = new CborBuilder();
        cborBuilder.addMap().put("fmt", "android-safetynet")
                .putMap("attStmt")
                .put("ver", "1234")
                .put("response", ("eyJraWQiOiI0YzQ2YzVlZi0zYWI0LTQyYWYtYTY0Yi04MzgwZjBlM2JiNmEiLCJhbGciOiJSUzI1NiJ9."
                        + "ew0KICAic3ViIjogIjEyMzQ1Njc4OTAiLA0KICAibmFtZSI6ICJkdW1teSIsDQogICJpYXQiOiAxNTE2MjM5MDIyD"
                        + "Qp9.snRTopaGtVaq2_IV5USBOZi3Fma_P4gpdq-7WjyLg1U6aCmaVoeTVJPGyLbh-vuzbU-UgkMlQUPzo9xA634tf"
                        + "x1PFwWRckx7Ibp_Xs-VMrESxIjAYlxZ76V4qMRx5Y5eWzhK7yhsRUqy9uoKpIEwEgX7QdfPL9SLKC-").getBytes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder cborEncoder = new CborEncoder(baos);
        cborEncoder.encode(cborBuilder.build());

        return baos.toByteArray();
    }

    private X509Certificate generateCertificate(String cn) throws NoSuchAlgorithmException, CertificateException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());

        java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return generateCertificate(cn, keyPair, 10, "SHA256withRSA");
    }

    private X509Certificate generateCertificate() throws NoSuchAlgorithmException, CertificateException {
        return generateCertificate("CN=attest.android.com");
    }

    private X509Certificate generateCertificate(String dn, KeyPair pair,
            int days, String algorithm)
            throws CertificateException {

        try {
            Security.addProvider(new BouncyCastleProvider());
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(algorithm);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(
                    pair.getPrivate().getEncoded());
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());
            ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);
            X500Name name = new X500Name(dn);
            Date from = new Date();
            Date to = new Date(from.getTime() + days * 86400000L);
            BigInteger sn = new BigInteger(64, new SecureRandom());

            X509v1CertificateBuilder v1CertGen = new X509v1CertificateBuilder(name, sn, from, to, name, subPubKeyInfo);
            X509CertificateHolder certificateHolder = v1CertGen.build(sigGen);
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
        } catch (CertificateException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CertificateException(e);
        }
    }
}
