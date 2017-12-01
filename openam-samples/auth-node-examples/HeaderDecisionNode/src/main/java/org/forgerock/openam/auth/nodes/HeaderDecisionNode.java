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
 * Copyright 2017 ForgeRock AS.
 */
// simon.moffatt@forgerock.com - checks given header name and header value

package org.forgerock.openam.auth.nodes;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;


@Node.Metadata(outcomeProvider = HeaderDecisionNode.OutcomeProvider.class,
        configClass = HeaderDecisionNode.Config.class)
public class HeaderDecisionNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the HTTP header you're looking for
         * @return the header name.
         */
        @Attribute(order = 100)
        default String headerName() {
            return "Name of Header";
        }

        /**
         * The vlaue of the HTTP header.
         * @return the header name.
         */
        @Attribute(order = 200)
        default String headerValue() {
            return "Value of Header";
        }
    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public HeaderDecisionNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        boolean headerPresent = context.request.headers.containsKey(config.headerName());
        boolean headerValuePresent = context.request.headers.get(config.headerName()).contains(config.headerValue());

        //Header found with correct value
        if (headerPresent && headerValuePresent) {

            return goTo("found").build();

        } else {

            return goTo("notFound").build();

        }

    }


    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = HeaderDecisionNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome("found", bundle.getString("found")),
                    new Outcome("notFound", bundle.getString("notFound")));
        }
    }
}