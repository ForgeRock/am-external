/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes 
 *
 */

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

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import java.util.*;

import static org.forgerock.openam.auth.node.api.Action.send;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>Places the result in the shared state as 'username'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = InputCollectorNode.Config.class)
public class InputCollectorNode extends SingleOutcomeNode {

    public interface Config {
        @Attribute(order = 100)
        Map<String, String> properties();
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/InputCollectorNode";
    private final static String DEBUG_FILE = "InputCollectorNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    private final InputCollectorNode.Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public InputCollectorNode(@Assisted InputCollectorNode.Config config) {
        this.config = config;
    }


    private String findCallbackValue(TreeContext context, String name) {
        Iterator<? extends Callback> iterator = context.getAllCallbacks().iterator();
        while (iterator.hasNext()) {
            NameCallback ncb = (NameCallback) iterator.next();
            if (name.equals(ncb.getPrompt())) return ncb.getName();
        }
        return "";
    }


    @Override
    public Action process(TreeContext context) {
        JsonValue newSharedState = context.sharedState.copy();
        Set<String> configKeys = config.properties().keySet();
        if (context.hasCallbacks()) {
            for (String key: configKeys) {
                String result = findCallbackValue(context, config.properties().get(key));
                newSharedState.put(key, result);
            }
            return goToNext().replaceSharedState(newSharedState).build();
        } else {
            List<Callback> callbacks = new ArrayList<Callback>(1);
            for (String key : configKeys) {
                NameCallback nameCallback = new NameCallback(config.properties().get(key));
                callbacks.add(nameCallback);
            }
            return send(ImmutableList.copyOf(callbacks)).build();

        }
    }
}
