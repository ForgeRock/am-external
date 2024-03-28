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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted.wrappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sun.identity.entitlement.EntitlementInfo;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.mockito.Mock;
import org.testng.annotations.DataProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class that verifies the conversion operations performed in the wrapper (Set to List conversions).
 */
public class ScriptEntitlementinfoWrapperTest {

    @Mock
    EntitlementInfo entitlementInfo;

    @BeforeClass
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @DataProvider(name = "requestedResourceNames")
    public Object[][] requestedResourceNames() {
        return new Object[][]{
                {null, null},
                {Set.of("testRequestedResource1", "testRequestedResource2"),
                        List.of("testRequestedResource1", "testRequestedResource2")},
                {Set.of(""), List.of("")}};
    }

    @Test(dataProvider = "requestedResourceNames")
    public void testGetRequestedResourceNames(Set<String> requestedResourceNames,
            List<String> expectedRequestedResourceNames) {
        when(entitlementInfo.getRequestedResourceNames()).thenReturn(requestedResourceNames);
        ScriptEntitlementInfoWrapper scriptEntitlementInfoWrapper = new ScriptEntitlementInfoWrapper(entitlementInfo);

        List<String> actualRequestedResourceNames = scriptEntitlementInfoWrapper.getRequestedResourceNames();
        if (expectedRequestedResourceNames == null) {
            assertThat(actualRequestedResourceNames == null);
        } else {
            assertThat(actualRequestedResourceNames).containsAll(expectedRequestedResourceNames);
        }
    }

    @DataProvider(name = "actionValues")
    public Object[][] actionValues() {
        return new Object[][]{
                {null, null},
                {Set.of("testActionValue1", "testActionValue2"), List.of("testActionValue1", "testActionValue2")},
                {Set.of(""), List.of("")}};
    }

    @Test(dataProvider = "actionValues")
    public void testGetActionValues(Set<Object> actionValues, List<String> expectedActionValues) {
        when(entitlementInfo.getActionValues(anyString())).thenReturn(actionValues);
        ScriptEntitlementInfoWrapper scriptEntitlementInfoWrapper = new ScriptEntitlementInfoWrapper(entitlementInfo);

        List<Object> actualActionValues = scriptEntitlementInfoWrapper.getActionValues("test");
        if (expectedActionValues == null) {
            assertThat(actualActionValues == null);
        } else {
            assertThat(actualActionValues).containsAll(expectedActionValues);
        }
    }

    @DataProvider(name = "advices")
    public Object[][] advices() {
        return new Object[][]{
                {null, null},
                {Map.of("advice1", Set.of("property1", "property2")),
                        Map.of("advice1", List.of("property1", "property2"))},
                {Map.of("advice2", Set.of("property1", "property2"), "advice3", Set.of("property1")),
                        Map.of("advice2", List.of("property1", "property2"), "advice3", List.of("property1"))},
                {Map.of("advice4", Set.of("")),
                        Map.of("advice4", List.of(""))}};
    }

    @Test(dataProvider = "advices")
    public void testAdvices(Map<String, Set<String>> advices, Map<String, List<String>> expectedAdvices) {
        when(entitlementInfo.getAdvices()).thenReturn(advices);
        ScriptEntitlementInfoWrapper scriptEntitlementInfoWrapper = new ScriptEntitlementInfoWrapper(entitlementInfo);

        Map<String, List<String>> actualAdvices = scriptEntitlementInfoWrapper.getAdvices();
        if (expectedAdvices == null) {
            assertThat(actualAdvices == null);
        } else {
            assertThat(actualAdvices.size()).isEqualTo(expectedAdvices.size());
            for (String expectedAdviceKey : expectedAdvices.keySet()) {
                assertThat(actualAdvices.containsKey(expectedAdviceKey)).isTrue();
                assertThat(actualAdvices.get(expectedAdviceKey)).isEqualTo(actualAdvices.get(expectedAdviceKey));
            }
        }
    }

    @DataProvider(name = "attributes")
    public Object[][] attributes() {
        return new Object[][]{
                {null, null},
                {Map.of("attribute1Key", Set.of("attribute1Value1", "attribute1Value2")),
                        Map.of("attribute1Key", List.of("attribute1Value1", "attribute1Value2"))},
                {Map.of("attribute2Key", Set.of("attribute1Value1", "attribute1Value2"),
                        "attribute3Key", Set.of("attribute3Value1")),
                        Map.of("attribute4Key", List.of("attribute4Value1", "attribute4Value2"),
                                "attribute5Key", List.of("attribute5Value1"))},
                {Map.of("attribute6Key", Set.of("")),
                        Map.of("attribute6Key", List.of(""))}};
    }

    @Test(dataProvider = "advices")
    public void testGetAttributes(Map<String, Set<String>> attributes, Map<String, List<String>> expectedAttributes) {
        when(entitlementInfo.getAttributes()).thenReturn(attributes);
        ScriptEntitlementInfoWrapper scriptEntitlementInfoWrapper = new ScriptEntitlementInfoWrapper(entitlementInfo);

        Map<String, List<String>> actualAttributes = scriptEntitlementInfoWrapper.getAttributes();
        if (expectedAttributes == null) {
            assertThat(actualAttributes == null);
        } else {
            assertThat(actualAttributes.size()).isEqualTo(expectedAttributes.size());
            for (String expectedAdviceKey : expectedAttributes.keySet()) {
                assertThat(actualAttributes.containsKey(expectedAdviceKey)).isTrue();
                assertThat(actualAttributes.get(expectedAdviceKey)).isEqualTo(actualAttributes.get(expectedAdviceKey));
            }
        }
    }

    @DataProvider(name = "resourceNames")
    public Object[][] resourceNames() {
        return new Object[][]{
                {null, null},
                {Set.of("testResource1", "testResource2"), List.of("testResource1", "testResource2")},
                {Set.of(""), List.of("")}};
    }

    @Test(dataProvider = "resourceNames")
    public void testGetResourceNames(Set<String> resourceNames, List<String> expectedResourceNames) {
        when(entitlementInfo.getResourceNames()).thenReturn(resourceNames);
        ScriptEntitlementInfoWrapper scriptEntitlementInfoWrapper = new ScriptEntitlementInfoWrapper(entitlementInfo);

        List<String> actualResourceNames = scriptEntitlementInfoWrapper.getResourceNames();
        if (expectedResourceNames == null) {
            assertThat(actualResourceNames == null);
        } else {
            assertThat(actualResourceNames).containsAll(expectedResourceNames);
        }
    }
}
