/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;

import javax.security.auth.callback.PasswordCallback;
import java.util.ResourceBundle;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which collects a password from the user via a password callback.
 *
 * <p>Places the result in the shared state as 'password'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = PasswordCollectorNode.Config.class)
public class PasswordCollectorNode extends SingleOutcomeNode {

    /**
     * Configuration for the password collector node.
     */
    public interface Config {
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/PasswordCollectorNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    @Override
    public Action process(TreeContext context) {
        logger.debug("PasswordCollectorNode started");
        JsonValue sharedState = context.sharedState;
        JsonValue transientState = context.transientState;
        return context.getCallback(PasswordCallback.class)
                .map(PasswordCallback::getPassword)
                .map(String::new)
                .filter(password -> !Strings.isNullOrEmpty(password))
                .map(password -> {
                    logger.debug("password has been collected and put in the shared state");
                    return goToNext()
                        .replaceSharedState(sharedState.copy())
                        .replaceTransientState(transientState.copy().put(PASSWORD, password)).build();
                })
                .orElseGet(() -> {
                    logger.debug("collecting password");
                    return collectPassword(context);
                });
    }

    private Action collectPassword(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        return send(new PasswordCallback(bundle.getString("callback.password"), false)).build();
    }
}
