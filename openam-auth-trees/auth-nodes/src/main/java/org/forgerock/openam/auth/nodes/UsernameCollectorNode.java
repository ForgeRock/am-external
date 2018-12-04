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
 * Copyright 2017-2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.ResourceBundle;

import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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
