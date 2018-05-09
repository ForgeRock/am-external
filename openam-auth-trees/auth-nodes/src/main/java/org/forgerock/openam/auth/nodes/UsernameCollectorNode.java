/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.ResourceBundle;

import javax.security.auth.callback.NameCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>Places the result in the shared state as 'username'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = UsernameCollectorNode.Config.class)
public class UsernameCollectorNode extends SingleOutcomeNode {

    /**
     * Configuration for the username collector node.
     */
    public interface Config {
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/UsernameCollectorNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    @Override
    public Action process(TreeContext context) {
        logger.debug("UsernameCollectorNode started");
        JsonValue sharedState = context.sharedState;
        return context.getCallback(NameCallback.class)
                .map(NameCallback::getName)
                .filter(password -> !Strings.isNullOrEmpty(password))
                .map(name -> goToNext().replaceSharedState(sharedState.copy().put(USERNAME, name)).build())
                .orElseGet(() -> collectUsername(context));
    }

    private Action collectUsername(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        logger.debug("collecting username");
        return send(new NameCallback(bundle.getString("callback.username"))).build();
    }
}
