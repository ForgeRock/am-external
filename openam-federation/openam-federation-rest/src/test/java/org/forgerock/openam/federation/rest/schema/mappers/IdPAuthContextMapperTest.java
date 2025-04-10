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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem.Key.AUTHLEVEL;
import static org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem.Key.MODULE;
import static org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem.Key.SERVICE;
import static org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem.Key.USER;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.openam.federation.rest.schema.hosted.identity.AuthContextItem;
import org.junit.jupiter.api.Test;

public class IdPAuthContextMapperTest {

    private IdpAuthContextMapper mapper = new IdpAuthContextMapper();
    private AuthContextItem authContextItem;

    @Test
    void shouldConvertSimpleEntry() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|3||"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getKey()).isNull();
        assertThat(entry.getValue()).isNull();
        assertThat(entry.getLevel()).isEqualTo(3);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
    }

    @Test
    void shouldConvertEntryWithoutLevel() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|||"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getKey()).isNull();
        assertThat(entry.getValue()).isNull();
        assertThat(entry.getLevel()).isEqualTo(0);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
    }

    @Test
    void shouldConvertEntryWithAllFieldsCompleted() {
        List<AuthContextItem> converted = mapper.map(
                singletonList("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|10|module=DataStore|default"), ROOT);
        assertThat(converted).hasSize(1);
        AuthContextItem entry = converted.get(0);
        assertThat(entry.getKey()).isEqualTo(MODULE);
        assertThat(entry.getValue()).isEqualTo("DataStore");
        assertThat(entry.getLevel()).isEqualTo(10);
        assertThat(entry.getContextReference())
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
    }

    @Test
    void shouldInverseMappedValue() {
        // Given
        authContextItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Password", MODULE, "value", 5);
        List<AuthContextItem> mappedValue = new ArrayList<>();
        mappedValue.add(authContextItem);
        // When
        List<String> actual = mapper.inverse(mappedValue, ROOT);
        // Then
        assertThat(actual.get(0)).isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|5|module=value|default");
    }

    @Test
    void shouldInverseNonDefaultValuesCorrectly() {
        // Given
        AuthContextItem defaultItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol",
                SERVICE,
                "yeah", 5);
        this.authContextItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Password", null, "", 5);
        List<AuthContextItem> mappedValue = new ArrayList<>();
        mappedValue.add(defaultItem);
        mappedValue.add(this.authContextItem);
        // When
        List<String> actual = mapper.inverse(mappedValue, ROOT);
        // Then
        assertThat(actual.get(1)).isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|5||");
    }

    @Test
    void shouldInverseEntryWithoutLevel() {
        // Given
        AuthContextItem defaultItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol",
                SERVICE,
                "yeah", 5);
        this.authContextItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Password", null, "", null);
        List<AuthContextItem> mappedValue = new ArrayList<>();
        mappedValue.add(defaultItem);
        mappedValue.add(this.authContextItem);
        // When
        List<String> actual = mapper.inverse(mappedValue, ROOT);
        // Then
        assertThat(actual.get(1)).isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password|0||");
    }

    @Test
    void shouldSortItemsSoThatDefaultIsFirst() {
        AuthContextItem defaultItem = new AuthContextItem(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport",
                USER,
                "Bert", 3);
        List<AuthContextItem> converted = mapper.map(Arrays.asList(
                authContextItemAsString(
                        new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol", SERVICE, "blah",
                                2),
                        false),
                authContextItemAsString(
                        new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Password", MODULE, "blah", 7),
                        false),
                authContextItemAsString(
                        defaultItem,
                        true),
                authContextItemAsString(
                        new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos", AUTHLEVEL, "2", 2),
                        false)), ROOT);
        assertThat(converted.get(0)).isEqualTo(defaultItem);
    }

    @Test
    void shouldMarkFirstItemAsDefaultDuringSerialisation() {
        AuthContextItem defaultItem = new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol",
                SERVICE,
                "blah",
                2);
        List<String> inverted = mapper.inverse(Arrays.asList(
                defaultItem,
                new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Password", MODULE, "blah", 7),
                new AuthContextItem("urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos", AUTHLEVEL, "2", 2)), ROOT);
        assertThat(inverted.get(0)).isEqualTo(authContextItemAsString(defaultItem, true));
    }

    private String authContextItemAsString(AuthContextItem item, boolean isDefault) {
        return String
                .format("%s|%d|%s=%s|%s", item.getContextReference(), item.getLevel(), item.getKey(), item.getValue(),
                        isDefault ? "default" : "");
    }
}
