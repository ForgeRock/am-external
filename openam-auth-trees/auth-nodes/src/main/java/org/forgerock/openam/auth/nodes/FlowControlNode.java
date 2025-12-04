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

import static org.forgerock.openam.auth.node.api.Action.goTo;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.PercentageValidator;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Node for splitting the flow of authentication journeys based on a configured percentage.
 */
@Node.Metadata(outcomeProvider = FlowControlNode.FlowControlOutcomeProvider.class,
        configClass = FlowControlNode.Config.class, tags = {"utilities"})
public class FlowControlNode implements Node {

    private static final Random GENERATOR = ThreadLocalRandom.current();
    private final Config config;

    @Inject
    FlowControlNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {
        int rand = GENERATOR.nextInt(0, 100);
        if (rand < config.percentageA()) {
            return goTo("A").build();
        } else {
            return goTo("B").build();
        }
    }

    /**
     * Outcome provider for the flow control node.
     */
    public static class FlowControlOutcomeProvider implements BoundedOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes)
                throws NodeProcessException {
            if (nodeAttributes.isNotNull()) {
                return List.of(new Outcome("A", nodeAttributes.get("outcomeADisplayName").asString()),
                        new Outcome("B", nodeAttributes.get("outcomeBDisplayName").asString()));
            } else {
                return getAllOutcomes(locales);
            }
        }

        @Override
        public List<Outcome> getAllOutcomes(PreferredLocales locales) throws NodeProcessException {
            return List.of(new Outcome("A", "A"), new Outcome("B", "B"));
        }
    }

    /**
     * Configuration for the flow control node.
     */
    public interface Config {

        /**
         * The percentage of requests that should go to outcome A, defaults to 50%. Must be between 0 and 100.
         * @return the percentage of requests that should go to outcome A.
         */
        @Attribute(order = 100, requiredValue = true, validators = {PercentageValidator.class})
        default int percentageA() {
            return 50;
        }

        /**
         * The name of the outcome that should be used for outcome A.
         * @return the name of the outcome that should be used for outcome A.
         */
        @Attribute(order = 200, requiredValue = true)
        default String outcomeADisplayName() {
            return "A";
        }

        /**
         * The name of the outcome that should be used for outcome B.
         * @return the name of the outcome that should be used for outcome B.
         */
        @Attribute(order = 300, requiredValue = true)
        default String outcomeBDisplayName() {
            return "B";
        }
    }
}
