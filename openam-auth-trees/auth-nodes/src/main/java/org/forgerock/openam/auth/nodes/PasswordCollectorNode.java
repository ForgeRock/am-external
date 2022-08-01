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
 * Copyright 2017-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;

import java.util.ResourceBundle;

import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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

    private static final String BUNDLE = PasswordCollectorNode.class.getName();
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
                    logger.debug("password has been collected and put in the transient state");
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
