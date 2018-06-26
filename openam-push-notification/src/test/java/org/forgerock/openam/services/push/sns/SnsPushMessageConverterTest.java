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
 * Copyright 2016-2018 ForgeRock AS.
 */
package org.forgerock.openam.services.push.sns;

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.BDDMockito.given;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SnsPushMessageConverterTest {

    @Mock
    private MessageId messageId;
    SnsPushMessageConverter converter = new SnsPushMessageConverter();

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldConvertToFormat() {

        //given
        given(messageId.toString()).willReturn("AUTHENTICATE:weasel");
        PushMessage pm = new PushMessage("recipient", "body", "subject", messageId);

        //when
        String result = converter.toTransferFormat(pm);

        //then

        JsonValue value = JsonValueBuilder.toJsonValue(result);

        assertThat(value.get("default")).isString().contains("subject");

        //GCM FORMAT CHECK =====

        assertThat(value.get("GCM")).isString();

        JsonValue gcmValue = JsonValueBuilder.toJsonValue(value.get("GCM").asString());

        assertThat(gcmValue.get("PRIORITY")).isString().isEqualTo("high");
        assertThat(gcmValue.get("data")).isNotNull();

        JsonValue gcmDataValue = gcmValue.get("data");

        assertThat(gcmDataValue.get("messageId")).isString().isEqualTo("AUTHENTICATE:weasel");
        assertThat(gcmDataValue.get("message")).isString().isEqualTo("body");

        //APNS FORMAT CHECK =====

        assertThat(value.get("APNS")).isString();
        JsonValue apnsValue = JsonValueBuilder.toJsonValue(value.get("APNS").asString());
        assertThat(apnsValue.get("aps")).isNotNull();
        JsonValue apnsDataValue = apnsValue.get("aps");
        assertThat(apnsDataValue.get("messageId")).isString().isEqualTo("AUTHENTICATE:weasel");
        assertThat(apnsDataValue.get("alert")).isString().isEqualTo("subject");
        assertThat(apnsDataValue.get("data")).isString().isEqualTo("body");
        assertThat(apnsDataValue.get("sound")).isString().isEqualTo("default");
    }

}
