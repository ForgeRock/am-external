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

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

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

/** A node that checks to see if zero-page login headers have specified username and password for this request. */
@Node.Metadata(outcomeProvider = ZeroPageLoginNode.OutcomeProvider.class,
        configClass = ZeroPageLoginNode.Config.class)
public class ZeroPageLoginNode implements Node {

    private final static String TRUE_OUTCOME_ID = "true";
    private final static String FALSE_OUTCOME_ID = "false";

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the HTTP header containing the username.
         * @return the header name.
         */
        @Attribute(order = 100)
        default String usernameHeader() {
            return "X-OpenAM-Username";
        }

        /**
         * The name of the HTTP header containing the password.
         * @return the header name.
         */
        @Attribute(order = 200)
        default String passwordHeader() {
            return "X-OpenAM-Password";
        }
    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public ZeroPageLoginNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        boolean hasUsername = context.request.headers.containsKey(config.usernameHeader());
        boolean hasPassword = context.request.headers.containsKey(config.passwordHeader());
        if (!hasUsername && !hasPassword) {
            return goTo(false).build();
        }
        JsonValue state = context.sharedState.copy();
        updateStateIfPresent(context, hasUsername, config.usernameHeader(), USERNAME, state);
        updateStateIfPresent(context, hasPassword, config.passwordHeader(), PASSWORD, state);
        return goTo(true).replaceSharedState(state).build();
    }

    private void updateStateIfPresent(TreeContext context, boolean hasValue, String headerName, String stateKey,
            JsonValue state) throws NodeProcessException {
        if (hasValue) {
            List<String> values = context.request.headers.get(headerName);
            if (values.size() != 1) {
                throw new NodeProcessException("Expecting only one header value for username and/or password");
            }
            state.put(stateKey, values.get(0));
        }
    }

    private Action.ActionBuilder goTo(boolean outcome) {
        return Action.goTo(outcome ? TRUE_OUTCOME_ID : FALSE_OUTCOME_ID);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = ZeroPageLoginNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString("trueOutcome")),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString("falseOutcome")));
        }
    }
}
