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
 * Copyright 2025 Ping Identity Corporation.
 */

package org.forgerock.openam.services.push.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Key;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelper;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelperFactory;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.SecretsProvider;
import org.forgerock.secrets.keys.SigningKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;

@ExtendWith(MockitoExtension.class)
public class PushDeviceUpdaterTest {

    @Mock
    private PushNotificationServiceConfigHelperFactory configHelperFactory;

    @Mock
    private UserPushDeviceProfileManager userPushDeviceProfileManager;

    @Mock
    private PushNotificationServiceConfigHelper configHelper;

    @Mock
    private JsonValue content;

    @InjectMocks
    private PushDeviceUpdater pushDeviceUpdater;

    @Test
    public void testValidateSignedJwt() throws Exception {
        // Given
        Key hmacKey = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
        var signingKey = new SecretBuilder()
                .stableId("keyId")
                .expiresAt(Instant.MAX)
                .secretKey(hmacKey)
                .build(Purpose.SIGN);
        var verificationKey = new SecretBuilder()
                .stableId("keyId")
                .expiresAt(Instant.MAX)
                .secretKey(hmacKey)
                .build(Purpose.VERIFY);

        String jwtToken = createJwt(signingKey);

        JsonPointer location = new JsonPointer("/jwt");

        when(content.get(location)).thenReturn(JsonValue.json(jwtToken));

        // When
        boolean result = pushDeviceUpdater.validateSignedJwt(content, verificationKey, location);

        // Then
        assertTrue(result);
    }

    @Test
    public void testUpdateDeviceSuccess() throws SSOException, SMSException {
        // Given
        when(configHelperFactory.getConfigHelperFor(anyString())).thenReturn(configHelper);

        // When
        boolean result = pushDeviceUpdater.updateDevice(content, "realm");

        // Then
        assertTrue(result);
    }

    @Test
    public void testUpdateDeviceFailure() throws SSOException, SMSException {
        // Given
        when(configHelperFactory.getConfigHelperFor(anyString())).thenThrow(new SMSException("error"));

        // When
        boolean result = pushDeviceUpdater.updateDevice(content, "realm");

        // Then
        assertFalse(result);
    }

    @Test
    public void testGetDeviceSettings() throws DevicePersistenceException {
        // Given
        PushDeviceSettings settings = createPushDeviceSettings();
        when(userPushDeviceProfileManager.getDeviceProfile(anyString(), anyString(), anyString()))
                .thenReturn(settings);

        // When
        PushDeviceSettings result = pushDeviceUpdater
                .getDeviceSettings("username", "mechanismUid", "realm");

        // Then
        assertEquals(settings, result);
    }

    @Test
    public void testSaveDeviceSettingsWithoutName() throws DevicePersistenceException {
        // Given
        PushDeviceSettings settings = createPushDeviceSettings();

        JsonValue deviceResponse = JsonValue.json(JsonValue.object(JsonValue.field("communicationId", "commId"),
                JsonValue.field("deviceId", "devId"), JsonValue.field("communicationType", "commType"),
                JsonValue.field("deviceType", "devType")));

        // When
        pushDeviceUpdater.saveDeviceSettings(settings, deviceResponse, "username", "realm");

        // Then
        verify(userPushDeviceProfileManager).saveDeviceProfile("username", "realm", settings);
        assertEquals("commId", settings.getCommunicationId());
        assertEquals("devId", settings.getDeviceId());
        assertEquals("commType", settings.getCommunicationType());
        assertEquals("devType", settings.getDeviceType());
        assertEquals("Push Device", settings.getDeviceName());
    }

    @Test
    public void testSaveDeviceSettingsWithName() throws DevicePersistenceException {
        // Given
        PushDeviceSettings settings = createPushDeviceSettings();

        JsonValue deviceResponse = JsonValue.json(JsonValue.object(JsonValue.field("communicationId", "commId"),
                JsonValue.field("deviceId", "devId"), JsonValue.field("communicationType", "commType"),
                JsonValue.field("deviceType", "devType"), JsonValue.field("deviceName", "devName")));

        // When
        pushDeviceUpdater.saveDeviceSettings(settings, deviceResponse, "username", "realm");

        // Then
        verify(userPushDeviceProfileManager).saveDeviceProfile("username", "realm", settings);
        assertEquals("commId", settings.getCommunicationId());
        assertEquals("devId", settings.getDeviceId());
        assertEquals("commType", settings.getCommunicationType());
        assertEquals("devType", settings.getDeviceType());
        assertEquals("devName", settings.getDeviceName());
    }

    private static PushDeviceSettings createPushDeviceSettings() {
        byte[] secretBytes = "secret".getBytes();
        String sharedSecret = Base64.encode(secretBytes);
        long currentTime = Time.currentTimeMillis();
        return new PushDeviceSettings(sharedSecret, "Push Device", currentTime, currentTime);
    }

    private String createJwt(SigningKey signingKey) {
        var signingHandler = new SigningManager(new SecretsProvider(Time.getClock()))
                .newSigningHandler(signingKey);
        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .claim("deviceName", "devName")
                .claim("deviceId", "devId")
                .claim("communicationId", "commId")
                .claim("deviceType", "devType")
                .claim("communicationType", "commType")
                .build();

        return new JwtBuilderFactory()
                .jws(signingHandler)
                .headers()
                .alg(JwsAlgorithm.HS256)
                .done()
                .claims(claims)
                .build();
    }
}