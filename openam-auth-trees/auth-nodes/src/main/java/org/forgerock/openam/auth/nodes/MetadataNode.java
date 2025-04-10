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

package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.MetadataCallback;

/**
 * A tree node that returns selected attributes from the shared state as metadata.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = MetadataNode.Config.class,
        tags = {"utilities"})
public class MetadataNode implements Node {
    /** When stream a map's entries we know there won't be any collisions so this is actually never called. */
    private static final BinaryOperator<Object> IGNORE_COLLISIONS = (a, b) -> a;

    /** Node configuration. */
    public interface Config {
        /**
         * The attributes that will be copied from the shared state.
         * @return The attribute names.
         */
        @Attribute(order = 100)
        Set<String> attributes();
    }

    private final Set<String> attributes;

    /**
     * DI constructor.
     * @param config The configuration of the node.
     */
    @Inject
    public MetadataNode(@Assisted Config config) {
        this.attributes = config.attributes();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Map<String, Object> data = context.sharedState.asMap().entrySet().stream()
                .filter(e -> attributes.contains(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, IGNORE_COLLISIONS, LinkedHashMap::new));
        return Action.send(new MetadataCallback(json(data))).build();
    }
}
