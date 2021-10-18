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
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_ENCRYPTED;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

/**
 * Tests for {@link OneTimePasswordNodeCommon}.
 */
public class OneTimePasswordNodeCommonTest {

    @Mock
    private NodeSharedStateCrypto nodeSharedStateCrypto;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private OneTimePasswordNodeCommon oneTimePasswordNodeCommon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(oneTimePasswordNodeCommon, nodeSharedStateCrypto);
    }

    @Test
    public void shouldGetClearTextOtp() {
        // Given
        String otp = "123456";
        TreeContext context = emptyTreeContext();
        JsonValue otpJsonValue = json(object(field(ONE_TIME_PASSWORD, otp)));
        context.sharedState.setObject(otpJsonValue);

        // When
        String clearTextOtp = oneTimePasswordNodeCommon.getClearTextOtp(context);

        // Then
        assertThat(clearTextOtp, is("123456"));
    }

    @Test
    public void shouldGetClearTextOtpGivenEncryptedOtpPayload() {
        // Given
        JsonValue encryptedOtp = json(object(field(ONE_TIME_PASSWORD_ENCRYPTED, "eyJEncryptedString")));
        TreeContext context = emptyTreeContext();
        context.sharedState.setObject(encryptedOtp);
        String otp = "123456";
        given(nodeSharedStateCrypto.decrypt(anyString())).willReturn(json(object(
                field(ONE_TIME_PASSWORD, otp),
                field(ONE_TIME_PASSWORD_TIMESTAMP, 15453734759L))));

        // When
        String clearTextOtp = oneTimePasswordNodeCommon.getClearTextOtp(context);

        // Then
        assertThat(clearTextOtp, is(otp));
    }

    @Test
    public void shouldFailToGetClearTextOtpGivenNullTreeContext() {
        // Given
        TreeContext context = null;

        try {
            // When
            oneTimePasswordNodeCommon.getClearTextOtp(context);

            // Then
            Assertions.shouldHaveThrown(NullPointerException.class);
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("tree context must not be null"));
        }
    }

}