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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.utils;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PushResponseUpdaterTest {
    @Mock
    PushNotificationService mockPushNotificationService;
    @Mock
    PushNotificationDelegate pushNotificationDelegate;
    @InjectMocks
    PushResponseUpdater responseUpdater;

    PushNotificationServiceConfig.Realm config = createConfig();
    String realm = "/";


    @Test
    void shouldSucceedWithValidInput() throws PushNotificationException {
        //given
        given(mockPushNotificationService.getDelegate(realm)).willReturn(pushNotificationDelegate);
        given(pushNotificationDelegate.createPlatformEndpoint(any(), any(), any()))
                .willReturn("endpointArn");
        JsonValue content = getContent();

        //when
        boolean result = responseUpdater.updateResponse(config, content, realm);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isTrue();

        assertThat(content.get("deviceId")).isString().contains("deviceId");
        assertThat(content.get("communicationType")).isString().contains("communicationType");
        assertThat(content.get("mechanismUid")).isString().contains("mechanismUid");
        assertThat(content.get("deviceType")).isString().contains("deviceType");
        assertThat(content.get("communicationId")).isString().contains("endpointArn");
    }


    @Test
    void shouldFailWithInvalidResponse() throws PushNotificationException {
        //given
        given(mockPushNotificationService.getDelegate(realm)).willReturn(pushNotificationDelegate);
        given(pushNotificationDelegate.createPlatformEndpoint(any(), any(), any()))
                .willReturn(null);

        JsonValue content = getContent();

        //when
        boolean result = responseUpdater.updateResponse(config, content, realm);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    @Test
    void shouldFailWhenNoJwtInContent() throws PushNotificationException {
        var realm = "/";
        //given
        JsonValue content = JsonValue.json(object(field("", "")));

        //when
        boolean result = responseUpdater.updateResponse(config, content, realm);

        //then
        org.assertj.core.api.Assertions.assertThat(result).isFalse();
    }

    private PushNotificationServiceConfig.Realm createConfig() {
        return new PushNotificationServiceConfig.Realm() {
            @Override
            public String delegateFactory() {
                return "delegateFactory";
            }

            @Override
            public char[] secret() {
                return "secret".toCharArray();
            }

            @Override
            public String appleEndpoint() {
                return "appleEndpoint";
            }

            @Override
            public String googleEndpoint() {
                return "googleEndpoint";
            }

            @Override
            public String region() {
                return "us-west-2";
            }

            @Override
            public String accessKey() {
                return "accessKey";
            }
        };
    }

    private static JsonValue getContent() {
        var jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim("communicationType", "communicationType")
                .claim("deviceId", "deviceId")
                .claim("mechanismUid", "mechanismUid")
                .claim("deviceType", "deviceType");

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newNopSigningHandler())
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.NONE).done().build();

        return json(object(field("jwt", jwt)));
    }

}
