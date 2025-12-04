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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.Collections;
import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.AttributeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test for {@link AttributeMapMapper}.
 *
 * @since 7.0.0
 */
public final class AttributeMapMapperTest {

    private static Object[][] attributesNamesAndValues() {
        return new Object[][]{
                {"abc=def", "abc", "def"},
                {"abc =def", "abc ", "def"},
                {"abc = def ", "abc ", " def "},
                {"abc=\" def \"", "abc", "\" def \""},
        };
    }

    @ParameterizedTest
    @MethodSource("attributesNamesAndValues")
    public void whenStringContainsAttributesNamesSamlAndLocalAttributesArePopulated(String serializeString,
            String attributeKey, String attributeValue) {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<String> values = Collections.singletonList(serializeString);

        // Given
        List<AttributeMap> attributeMaps = mapper.map(values, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0).getSamlAttribute()).isEqualTo(attributeKey);
        assertThat(attributeMaps.get(0).getLocalAttribute()).isEqualTo(attributeValue);
    }

    @Test
    void whenStringContainsNameFormatUriItIsPopulated() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<String> values = Collections.singletonList("123:456|abc=def");

        // Given
        List<AttributeMap> attributeMaps = mapper.map(values, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0).getNameFormatUri()).isEqualTo("123:456");
    }

    @Test
    void whenStringContainsBinaryDefinitionItIsPopulated() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<String> values = Collections.singletonList("123:456|abc=def;binary");

        // Given
        List<AttributeMap> attributeMaps = mapper.map(values, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0).getBinary()).isTrue();
    }

    @Test
    void whenAttributeContainsNoNameFormatUriItIsSkipped() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<AttributeMap> maps = Collections.singletonList(new AttributeMap(null, "456", "789", false));

        // Given
        List<String> attributeMaps = mapper.inverse(maps, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0)).isEqualTo("456=789");
    }

    @Test
    void whenAttributeContainsNameFormatUriItIsPopulated() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<AttributeMap> maps = Collections.singletonList(new AttributeMap("123", "456", "789", false));

        // Given
        List<String> attributeMaps = mapper.inverse(maps, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0)).isEqualTo("123|456=789");
    }

    @Test
    void whenAttributeContainsBinaryDefItIsPopulated() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<AttributeMap> maps = Collections.singletonList(new AttributeMap("123", "456", "789", true));

        // Given
        List<String> attributeMaps = mapper.inverse(maps, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0)).isEqualTo("123|456=789;binary");
    }

    @Test
    void shouldInverseAttributeMapWhenBinaryIsNull() {
        // When
        AttributeMapMapper mapper = new AttributeMapMapper();
        List<AttributeMap> maps = Collections.singletonList(new AttributeMap("123", "456", "789", null));

        // Given
        List<String> attributeMaps = mapper.inverse(maps, ROOT);

        // Then
        assertThat(attributeMaps).hasSize(1);
        assertThat(attributeMaps.get(0)).isEqualTo("123|456=789");
    }
}
