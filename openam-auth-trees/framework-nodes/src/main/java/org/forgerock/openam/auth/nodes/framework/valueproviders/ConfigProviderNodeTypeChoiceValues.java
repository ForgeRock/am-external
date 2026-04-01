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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.ConfigProviderEnabled;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.feature.Feature;
import org.forgerock.openam.feature.Features;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.ChoiceValues;

/**
 * Provide node type choice values for the nodes that are enabled for the configuration provider.
 * This includes the following node types:
 * <ul>
 *     <li>Nodes with an outcome provider that extends {@link StaticOutcomeProvider}</li>
 *     <li>Nodes with an outcome provider that extends {@link BoundedOutcomeProvider}</li>
 *     <li>Nodes with the {@link ConfigProviderEnabled} annotation</li>
 * </ul>
 */
public class ConfigProviderNodeTypeChoiceValues extends ChoiceValues {

    private final NodeRegistry nodeRegistry;
    private final AMResourceBundleCache resourceBundleCache;
    private final Feature dynamicConfigProviderFeature;

    @Inject
    ConfigProviderNodeTypeChoiceValues(NodeRegistry nodeRegistry,
            @Named("AMResourceBundleCache") AMResourceBundleCache resourceBundleCache,
            Features features) {
        this.nodeRegistry = nodeRegistry;
        this.resourceBundleCache = resourceBundleCache;
        this.dynamicConfigProviderFeature = features.dynamicConfigProviderEnabled();
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(Collections.emptyMap());
    }

    @Override
    public Map<String, String> getChoiceValues(Map<String, Object> envParams) {
        return nodeRegistry.getNodeTypeServiceNames().stream()
                   .filter(this::isConfigProviderEnabled)
                   .map(nodeTypeName -> {
                       var versions = nodeRegistry.getVersions(nodeTypeName);
                       return nodeNameFormatter(nodeTypeName, nodeRegistry.getNodeType(nodeTypeName,
                                       versions.get(versions.size() - 1)),
                               (Locale) envParams.getOrDefault(Constants.LOCALE, Locale.ROOT));
                   })
                   .sorted(Map.Entry.comparingByValue())
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private boolean isConfigProviderEnabled(String nodeTypeName) {
        var versions = nodeRegistry.getVersions(nodeTypeName);
        final Class<? extends Node> nodeType = nodeRegistry.getNodeType(nodeTypeName,
                versions.get(versions.size() - 1));
        final Class<? extends OutcomeProvider> outcomeProvider =
                requireNonNull(nodeType.getAnnotation(Node.Metadata.class),
                        "The outcome provider must not be null").outcomeProvider();
        return StaticOutcomeProvider.class.isAssignableFrom(outcomeProvider)
                       || BoundedOutcomeProvider.class.isAssignableFrom(outcomeProvider)
                       || (dynamicConfigProviderFeature.isEnabled()
                                   && nodeType.isAnnotationPresent(ConfigProviderEnabled.class));
    }

    private Map.Entry<String, String> nodeNameFormatter(String nodeType, Class<?> nodeClass, Locale locale) {
        String name = requireNonNull(nodeClass.getAnnotation(Node.Metadata.class), "The i18nFile must not be null")
                .i18nFile();
        if (name.isBlank()) {
            name = nodeClass.getName();
        }
        return Map.entry(nodeType, resourceBundleCache.getResBundle(name, locale).getString("nodeDescription"));
    }

}
