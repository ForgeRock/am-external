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

package com.sun.identity.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;

public class AttributeMapperPluginHelperTest {

    private AttributeMapperPluginHelper pluginHelper;

    @BeforeMethod
    public void setUp() {
        pluginHelper = new AttributeMapperPluginHelper();
    }

    @Test(expectedExceptions = SAML2Exception.class, expectedExceptionsMessageRegExp = "Null input.")
    public void shouldForbidNullNameArguments() throws SAML2Exception {
        // Given, When, Then
        pluginHelper.createSAMLAttribute(null, "nameFormat", Collections.singleton("value"));
    }

    @Test
    public void shouldReturnAttributeContainingProvidedName() throws SAML2Exception {
        // Given
        String name = "attrName";
        // When
        Attribute actual = pluginHelper.createSAMLAttribute(name, null, null);
        // Then
        assertThat(actual.getName()).isEqualTo(name);
    }

    @Test
    public void shouldReturnAttributeContainingProvidedNameFormat() throws SAML2Exception {
        // Given
        String name = "attrName";
        String nameFormat = "nameFormat";
        // When
        Attribute actual = pluginHelper.createSAMLAttribute(name, nameFormat, null);
        // Then
        assertThat(actual.getNameFormat()).isEqualTo(nameFormat);
    }

    @Test
    public void shouldReturnAttributeContainingProvidedAttributeValues() throws SAML2Exception {
        // Given
        String name = "attrName";
        Set<String> values = CollectionUtils.asSet("value1", "value2");
        // When
        Attribute actual = pluginHelper.createSAMLAttribute(name, null, values);
        // Then
        assertThat(actual.getAttributeValueString()).asList().containsOnly("value1", "value2");
    }

    @Test
    public void shouldReturnAttributeContainingProvidedArguments() throws SAML2Exception {
        // Given
        String name = "attrName";
        String nameFormat = "nameFormat";
        Set<String> values = CollectionUtils.asSet("value1", "value2");
        // When
        Attribute actual = pluginHelper.createSAMLAttribute(name, nameFormat, values);
        // Then
        assertThat(actual.getName()).isEqualTo(name);
        assertThat(actual.getNameFormat()).isEqualTo(nameFormat);
        assertThat(actual.getAttributeValueString()).containsExactlyInAnyOrder("value1", "value2");
    }

    @Test
    public void shouldReturnNullWhenProvidedBinaryValueMapIsNull() {
        // Given
        String samlAttribute = "samlAttribute";
        String localAttribute = "localAttribute";
        Map<String, byte[][]> binaryValueMap = null;
        // When
        Set<String> actual = pluginHelper.getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
        // Then
        assertThat(actual).isNull();
    }

    @Test
    public void shouldReturnNullWhenProvidedBinaryValueMapIsEmpty() {
        // Given
        String samlAttribute = "samlAttribute";
        String localAttribute = "localAttribute";
        Map<String, byte[][]> binaryValueMap = Collections.emptyMap();
        // When
        Set<String> actual = pluginHelper.getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
        // Then
        assertThat(actual).isNull();
    }

    @Test
    public void shouldReturnNullWhenLocalAttributeHasNoValueOnBinaryValueMap() {
        // Given
        String samlAttribute = "samlAttribute";
        String localAttribute = "localAttribute";
        Map<String, byte[][]> binaryValueMap = Map.of("item", new byte[1][2]);
        // When
        Set<String> actual = pluginHelper.getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
        // Then
        assertThat(actual).isNull();
    }

    @Test
    public void shouldReturnABase64EncodedSetOfLocalAttributeValues() {
        // Given
        String samlAttribute = "samlAttribute";
        String localAttribute = "localAttribute";
        Map<String, byte[][]> binaryValueMap = new HashMap<>();
        byte[][] bytes = new byte[2][1];
        bytes[0][0] = 1;
        bytes[1][0] = 2;
        binaryValueMap.put(localAttribute, bytes);
        // When
        Set<String> actual = pluginHelper.getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
        // Then
        assertThat(actual).containsExactlyInAnyOrder("Ag==", "AQ==");
    }

}