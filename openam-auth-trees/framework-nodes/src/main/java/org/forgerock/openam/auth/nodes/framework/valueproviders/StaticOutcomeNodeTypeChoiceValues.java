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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import static org.forgerock.am.trees.api.NodeRegistry.DEFAULT_VERSION;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.ChoiceValues;

/**
 * Provide node type choice values for the nodes that use the {@link StaticOutcomeProvider}.
 */
public class StaticOutcomeNodeTypeChoiceValues extends ChoiceValues {

    private final NodeRegistry nodeRegistry;
    private final AMResourceBundleCache resourceBundleCache;

    @Inject
    StaticOutcomeNodeTypeChoiceValues(NodeRegistry nodeRegistry,
            @Named("AMResourceBundleCache") AMResourceBundleCache resourceBundleCache) {
        this.nodeRegistry = nodeRegistry;
        this.resourceBundleCache = resourceBundleCache;
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(Collections.emptyMap());
    }

    @Override
    public Map<String, String> getChoiceValues(Map<String, Object> envParams) {
        return nodeRegistry.getNodeTypeServiceNames().stream()
                .filter(nodeTypeName -> {
                    // TODO - AME-27985 upgrade config provider to handle node type versioning
                    final Class<? extends Node> nodeType = nodeRegistry.getNodeType(nodeTypeName, DEFAULT_VERSION);
                    final Class<? extends OutcomeProvider> outcomeProvider = nodeType.getAnnotation(Node.Metadata.class)
                            .outcomeProvider();
                    return StaticOutcomeProvider.class.isAssignableFrom(outcomeProvider)
                                   || BoundedOutcomeProvider.class.isAssignableFrom(outcomeProvider);
                })
                .map(nodeTypeName -> nodeNameFormatter(nodeTypeName, nodeRegistry.getNodeType(nodeTypeName,
                                DEFAULT_VERSION), (Locale) envParams.getOrDefault(Constants.LOCALE, Locale.ROOT)))
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private Map.Entry<String, String> nodeNameFormatter(String nodeType, Class<?> nodeClass, Locale locale) {
        String name = nodeClass.getAnnotation(Node.Metadata.class).i18nFile();
        if (name.isBlank()) {
            name = nodeClass.getName();
        }
        return Map.entry(nodeType, resourceBundleCache.getResBundle(name, locale).getString("nodeDescription"));
    }

}
