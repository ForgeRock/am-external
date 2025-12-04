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
package org.forgerock.openam.auth.nodes;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that can add set of attributes to the shared state.
 */
@Node.Metadata(
        outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetStateNode.Config.class,
        tags = {"utilities"})
public class SetStateNode extends SingleOutcomeNode {
    private final Config config;

    /**
     * Create the node.
     *
     * @param config the service config.
     */
    @Inject
    public SetStateNode(@Assisted Config config) {
        this.config = config;
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The names and values of the state field to add.
         *
         * @return the attributes to add.
         */
        @Attribute(order = 100)
        Map<String, String> attributes();
    }


    @Override
    public Action process(TreeContext context) {
        for (var entry : config.attributes().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            var state = context.getStateFor(this);
            state.remove(key);
            state.putShared(key, value);
        }
        return goToNext().build();
    }

    @Override
    public OutputState[] getOutputs() {
        return config.attributes().keySet().stream().map(OutputState::new).toArray(OutputState[]::new);
    }
}
