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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.ConfigProviderEnabled;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.feature.Feature;
import org.forgerock.openam.feature.Features;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.shared.locale.AMResourceBundleCache;

@ExtendWith(MockitoExtension.class)
class ConfigProviderNodeTypeChoiceValuesTest {

    private ConfigProviderNodeTypeChoiceValues configProviderNodeTypeChoiceValues;
    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private AMResourceBundleCache resourceBundleCache;
    @Mock
    private Features features;
    @Mock
    private Feature dynamicConfigProviderFeature;

    @BeforeEach
    void setUp() {
        given(features.dynamicConfigProviderEnabled()).willReturn(dynamicConfigProviderFeature);
        configProviderNodeTypeChoiceValues = new ConfigProviderNodeTypeChoiceValues(nodeRegistry, resourceBundleCache,
                features);
    }

    @Test
    void testGetChoiceValuesReturnsAllEnabledNodeTypes() {
        //given
        given(dynamicConfigProviderFeature.isEnabled()).willReturn(true);
        given(nodeRegistry.getNodeTypeServiceNames()).willReturn(Set.of("MyTestNode", "MyTestNode2", "MyTestNode3",
                "MyTestNode4"));
        given(nodeRegistry.getNodeType("MyTestNode", 2)).willAnswer(inv -> StaticOutcomeNodeV2.class);
        given(nodeRegistry.getNodeType("MyTestNode2", 1)).willAnswer(inv -> VariableOutcomeNode.class);
        given(nodeRegistry.getNodeType("MyTestNode3", 1)).willAnswer(inv -> BoundedOutcomeNode.class);
        given(nodeRegistry.getNodeType("MyTestNode4", 1)).willAnswer(inv -> ConfigProviderEnabledNode.class);
        given(nodeRegistry.getVersions("MyTestNode")).willReturn(List.of(1, 2));
        given(nodeRegistry.getVersions("MyTestNode2")).willReturn(List.of(1));
        given(nodeRegistry.getVersions("MyTestNode3")).willReturn(List.of(1));
        given(nodeRegistry.getVersions("MyTestNode4")).willReturn(List.of(1));

        given(resourceBundleCache.getResBundle(any(), any())).willAnswer(inv -> new TestResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return inv.getArgument(0);
            }
        });
        //when
        var results = configProviderNodeTypeChoiceValues.getChoiceValues();
        //then
        assertThat(results).hasSize(3)
                .containsEntry("MyTestNode", "Static Outcome Node V2")
                .containsEntry("MyTestNode3", "Bounded Outcome Node")
                .containsEntry("MyTestNode4", "Config Provider Enabled Variable Node");
    }

    @Test
    void testReturnsLatestNodeType() {
        //given
        given(nodeRegistry.getNodeTypeServiceNames()).willReturn(Set.of("MyTestNode"));
        given(nodeRegistry.getNodeType("MyTestNode", 2)).willAnswer(inv -> StaticOutcomeNodeV2.class);
        given(nodeRegistry.getVersions("MyTestNode")).willReturn(List.of(1, 2));

        given(resourceBundleCache.getResBundle(any(), any())).willAnswer(inv -> new TestResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return inv.getArgument(0);
            }
        });

        //when
        var results = configProviderNodeTypeChoiceValues.getChoiceValues();

        //then
        assertThat(results).hasSize(1)
                .containsEntry("MyTestNode", "Static Outcome Node V2");
    }

    @Test
    void testGetChoiceValuesDoesNotReturnConfigProviderEnabledNodeWhenFeatureDisabled() {
        //given
        given(dynamicConfigProviderFeature.isEnabled()).willReturn(false);
        given(nodeRegistry.getNodeTypeServiceNames()).willReturn(Set.of("MyTestNode", "MyTestNode2", "MyTestNode3",
                "MyTestNode4"));
        given(nodeRegistry.getNodeType("MyTestNode", 2)).willAnswer(inv -> StaticOutcomeNodeV2.class);
        given(nodeRegistry.getNodeType("MyTestNode2", 1)).willAnswer(inv -> VariableOutcomeNode.class);
        given(nodeRegistry.getNodeType("MyTestNode3", 1)).willAnswer(inv -> BoundedOutcomeNode.class);
        given(nodeRegistry.getNodeType("MyTestNode4", 1)).willAnswer(inv -> ConfigProviderEnabledNode.class);
        given(nodeRegistry.getVersions("MyTestNode")).willReturn(List.of(1, 2));
        given(nodeRegistry.getVersions("MyTestNode2")).willReturn(List.of(1));
        given(nodeRegistry.getVersions("MyTestNode3")).willReturn(List.of(1));
        given(nodeRegistry.getVersions("MyTestNode4")).willReturn(List.of(1));
        given(resourceBundleCache.getResBundle(any(), any())).willAnswer(inv -> new TestResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return inv.getArgument(0);
            }
        });
        //when
        var results = configProviderNodeTypeChoiceValues.getChoiceValues();
        //then
        assertThat(results).hasSize(2)
                .containsEntry("MyTestNode", "Static Outcome Node V2")
                .containsEntry("MyTestNode3", "Bounded Outcome Node");
    }

    public static class Config {
    }

    @Node.Metadata(outcomeProvider = OutcomeProvider.class, configClass = Config.class,
            i18nFile = "Variable Outcome Node")
    private static class VariableOutcomeNode implements Node {
        @Override
        public Action process(TreeContext context) {
            return null;
        }
    }

    @Node.Metadata(outcomeProvider = OutcomeProvider.class, configClass = Config.class,
            i18nFile = "Config Provider Enabled Variable Node")
    @ConfigProviderEnabled
    private static class ConfigProviderEnabledNode implements Node {
        @Override
        public Action process(TreeContext context) {
            return null;
        }
    }

    @Node.Metadata(outcomeProvider = BoundedOutcomeNode.SomeOutcomeProvider.class, configClass = Config.class,
            i18nFile = "Bounded Outcome Node")
    private static class BoundedOutcomeNode implements Node {
        @Override
        public Action process(TreeContext context) {
            return null;
        }

        public static class SomeOutcomeProvider implements BoundedOutcomeProvider {

            @Override
            public List<Outcome> getAllOutcomes(PreferredLocales locales) {
                return List.of();
            }

            @Override
            public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
                return List.of();
            }
        }
    }

    @Node.Metadata(outcomeProvider = StaticOutcomeProvider.class, configClass = Config.class,
            i18nFile = "Static Outcome Node V2")
    @Node.VersionMetadata(name = "MyTestNode", version = 2)
    private static class StaticOutcomeNodeV2 implements Node {

        @Override
        public Action process(TreeContext context) {
            return null;
        }
    }

    @SuppressWarnings("NullableProblems")
    private abstract static class TestResourceBundle extends ResourceBundle {

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    }

}
