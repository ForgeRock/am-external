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

package com.sun.identity.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class CreateSAML2HostedProviderTemplateTest {

    @Test
    public void shouldAdvertiseSupportForGCMEncryptionAlgorithmsInIDPTemplate() {
        // Given
        StringBuffer xmlBuffer = new StringBuffer();

        // When
        CreateSAML2HostedProviderTemplate.addIdentityProviderTemplate(xmlBuffer, "test", "http://test.com/",
                null, "dummy-cert");

        // Then
        assertThat(xmlBuffer.toString())
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes256-gcm\">")
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes192-gcm\">")
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\">");
    }

    @Test
    public void shouldAdvertiseSupportForGCMEncryptionAlgorithmsInSPTemplate() {
        // Given
        StringBuffer xmlBuffer = new StringBuffer();

        // When
        CreateSAML2HostedProviderTemplate.addServiceProviderTemplate(xmlBuffer, "test", "http://test.com/",
                null, "dummy-cert");

        // Then
        assertThat(xmlBuffer.toString())
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes256-gcm\">")
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes192-gcm\">")
                .contains("<EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\">");
    }
}