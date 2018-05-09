/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/** A node that checks to see if zero-page login headers have specified username and password for this request. */
@Node.Metadata(outcomeProvider = ZeroPageLoginNode.OutcomeProvider.class,
        configClass = ZeroPageLoginNode.Config.class)
public class ZeroPageLoginNode implements Node {

    private final static String TRUE_OUTCOME_ID = "true";
    private final static String FALSE_OUTCOME_ID = "false";
    private static final String REFERER_HEADER_KEY = "referer";

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

        /**
         * Sets the node to allow requests to authenticate without a referer.
         *
         * @return the allow without referer flag.
         */
        @Attribute(order = 300)
        default boolean allowWithoutReferer() {
            return true;
        }

        /**
         * A white list of allowed referers. If a referer is required, the request must have a referer on this list.
         *
         * @return the referer white list.
         */
        @Attribute(order = 400)
        default Set<String> referrerWhiteList() {
            return new HashSet<>();
        }
    }

    private final Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

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
        logger.debug("ZeroPageLoginNode started");
        boolean hasUsername = context.request.headers.containsKey(config.usernameHeader());
        boolean hasPassword = context.request.headers.containsKey(config.passwordHeader());
        if (!hasUsername && !hasPassword) {
            logger.debug("no username or password set");
            return goTo(false).build();
        }
        boolean hasReferer = context.request.headers.containsKey(REFERER_HEADER_KEY);
        if (!config.allowWithoutReferer()) {
            if (!hasReferer || !isOnWhiteList(context.request.headers.get(REFERER_HEADER_KEY))) {
                return goTo(false).build();
            }
        }
        JsonValue sharedState = context.sharedState.copy();
        JsonValue transientState = context.transientState.copy();
        updateStateIfPresent(context, hasUsername, config.usernameHeader(), USERNAME, sharedState);
        updateStateIfPresent(context, hasPassword, config.passwordHeader(), PASSWORD, transientState);
        logger.debug("username {} and password set in sharedState", config.usernameHeader());
        return goTo(true).replaceSharedState(sharedState).replaceTransientState(transientState).build();
    }

    private void updateStateIfPresent(TreeContext context, boolean hasValue, String headerName, String stateKey,
            JsonValue state) throws NodeProcessException {
        if (hasValue) {
            List<String> values = context.request.headers.get(headerName);
            if (values.size() != 1) {
                logger.error("expecting only one header value for username and/or password but size is {}",
                        values.size());
                throw new NodeProcessException("Expecting only one header value for username and/or password "
                        + "but size is" + values.size());
            }
            state.put(stateKey, values.get(0));
        }
    }

    private Action.ActionBuilder goTo(boolean outcome) {
        return Action.goTo(outcome ? TRUE_OUTCOME_ID : FALSE_OUTCOME_ID);
    }

    private boolean isOnWhiteList(List<String> referers) {
        Set<String> configReferers = config.referrerWhiteList();
        for (String referer : referers) {
            if (configReferers.contains(referer)) {
                return true;
            }
        }
        return false;
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
